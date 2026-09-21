package com.bancada.service;

import com.bancada.enums.MachineDistribution;
import com.bancada.hyperv.CloudImage;
import com.bancada.hyperv.Qcow2Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Base disks of the distributions, kept on this PC. The first machine of a version downloads the
 * official cloud image, checks it against the checksum the distribution publishes, turns it into a
 * dynamic VHDX and keeps it; the next machines only copy it.
 */
@Service
public class MachineImageService {

    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofHours(2);
    private static final int PROGRESS_STEP_PERCENT = 10;

    private final HttpClient httpClient;
    private final HyperVService hyperVService;
    private final Path folder;
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public MachineImageService(HttpClient httpClient, HyperVService hyperVService, @Value("${bancada.data-dir}") String dataDir) {
        this.httpClient = httpClient;
        this.hyperVService = hyperVService;
        this.folder = Paths.get(dataDir, "imagens");
    }

    public Path baseDisk(MachineDistribution distribution, String version) {
        return folder.resolve(key(distribution, version)).resolve("base.vhdx");
    }

    public boolean isCached(MachineDistribution distribution, String version) {
        return Files.isRegularFile(baseDisk(distribution, version));
    }

    /** The base disk, downloading and converting it first when this PC does not have it yet. */
    public Path ensureBaseDisk(MachineDistribution distribution, String version, Consumer<String> log) {
        String key = key(distribution, version);
        synchronized (locks.computeIfAbsent(key, ignored -> new Object())) {
            Path base = baseDisk(distribution, version);
            if (Files.isRegularFile(base)) {
                log.accept("== Imagem " + distribution.getDisplayName() + " " + version + " já está neste PC ==\n");
                return base;
            }
            CloudImage image = distribution.imageFor(version);
            Path work = base.getParent();
            Path download = work.resolve("imagem.qcow2.part");
            Path vhd = work.resolve("imagem.vhd");
            Path converted = work.resolve("base-nova.vhdx");
            try {
                Files.createDirectories(work);
                Files.deleteIfExists(vhd);
                Files.deleteIfExists(converted);
                String expected = expectedHash(image);
                log.accept("== Baixando " + image.fileName() + " ==\n");
                String actual = download(image, download, log);
                if (!expected.equals(actual)) {
                    Files.deleteIfExists(download);
                    throw new IllegalStateException("A imagem baixada não confere com o checksum publicado pela distribuição. Tente de novo.");
                }
                log.accept("Checksum conferido (" + image.algorithm() + ").\n== Preparando o disco ==\n");
                Qcow2Image.toFixedVhd(download, vhd, bytes -> { });
                Files.delete(download);
                if (hyperVService.convertToVhdx(vhd, converted, log) != 0 || !Files.isRegularFile(converted)) {
                    throw new IllegalStateException("O Hyper-V não converteu a imagem para VHDX.");
                }
                Files.move(converted, base, StandardCopyOption.ATOMIC_MOVE);
                return base;
            } catch (IOException exception) {
                throw new IllegalStateException("Não foi possível preparar a imagem: " + exception.getMessage(), exception);
            } finally {
                deleteQuietly(vhd);
                deleteQuietly(converted);
            }
        }
    }

    private String expectedHash(CloudImage image) throws IOException {
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(image.checksumUrl())).timeout(Duration.ofMinutes(1)).build(),
            HttpResponse.BodyHandlers.ofString());
        String hash = response.statusCode() == 200 ? image.expectedHash(response.body()) : null;
        if (hash == null) {
            throw new IllegalStateException("A distribuição não publicou o checksum de " + image.fileName() + " (" + image.checksumUrl() + ").");
        }
        return hash;
    }

    /** Streams the image to disk while hashing it; returns the hex digest. */
    private String download(CloudImage image, Path target, Consumer<String> log) throws IOException {
        HttpResponse<InputStream> response = send(HttpRequest.newBuilder(URI.create(image.url())).timeout(DOWNLOAD_TIMEOUT).build(),
            HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IllegalStateException("O servidor da distribuição respondeu " + response.statusCode() + " para " + image.url());
        }
        long total = response.headers().firstValueAsLong("content-length").orElse(-1);
        MessageDigest digest = digest(image.algorithm());
        try (InputStream input = response.body(); OutputStream output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[1 << 16];
            long done = 0;
            int nextPercent = PROGRESS_STEP_PERCENT;
            int count;
            while ((count = input.read(buffer)) > 0) {
                output.write(buffer, 0, count);
                digest.update(buffer, 0, count);
                done += count;
                if (total > 0 && done * 100 / total >= nextPercent) {
                    log.accept(String.format(Locale.ROOT, "  %d%% (%d de %d MB)%n", nextPercent, done >> 20, total >> 20));
                    nextPercent += PROGRESS_STEP_PERCENT;
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return httpClient.send(request, handler);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrompido.", exception);
        }
    }

    private static MessageDigest digest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String key(MachineDistribution distribution, String version) {
        return distribution.name().toLowerCase(Locale.ROOT) + "-" + version;
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // leftover of a failed run, replaced next time
        }
    }
}
