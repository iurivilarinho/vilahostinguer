package com.bancada.service;

import com.bancada.models.Device;
import com.bancada.records.CommandResult;
import com.bancada.response.FileEntryResponse;
import com.bancada.response.FileUploadResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * File manager over plain shell commands instead of SFTP: dropbear, common on phones and small
 * boards, often ships without an sftp-server, and exec works everywhere.
 */
@Service
public class FileService {

    private static final Duration LIST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SIMPLE_TIMEOUT = Duration.ofSeconds(20);

    private final DeviceService deviceService;
    private final SshService sshService;
    private final DeviceScriptService deviceScriptService;

    public FileService(DeviceService deviceService, SshService sshService, DeviceScriptService deviceScriptService) {
        this.deviceService = deviceService;
        this.sshService = sshService;
        this.deviceScriptService = deviceScriptService;
    }

    public List<FileEntryResponse> list(Long deviceId, String path) {
        Device device = deviceService.requireReady(deviceId);
        String directory = normalize(path);
        CommandResult result = sshService.run(device, deviceScriptService.load("files"), List.of(directory), true, LIST_TIMEOUT);
        if (!result.succeeded()) {
            throw new IllegalArgumentException(firstLine(result.errorOutput(), "Não foi possível abrir " + directory));
        }
        List<FileEntryResponse> entries = new ArrayList<>();
        for (String line : result.output().split("\n")) {
            String[] fields = line.split("\\|", 5);
            if (fields.length < 5) {
                continue;
            }
            String name = fields[4];
            String fullPath = directory.endsWith("/") ? directory + name : directory + "/" + name;
            long modifiedEpoch = parseLong(fields[3]);
            entries.add(new FileEntryResponse(name, fullPath, "d".equals(fields[0]), "l".equals(fields[0]), parseLong(fields[1]),
                fields[2], LocalDateTime.ofInstant(Instant.ofEpochSecond(modifiedEpoch), ZoneId.systemDefault())));
        }
        entries.sort(Comparator.comparing(FileEntryResponse::directory).reversed()
            .thenComparing(entry -> entry.name().toLowerCase()));
        return entries;
    }

    /** Checked before a download starts: once bytes are streaming, an error can no longer become a 4xx. */
    public long fileSize(Long deviceId, String path) {
        Device device = deviceService.requireReady(deviceId);
        String file = normalize(path);
        CommandResult result = sshService.run(device, "[ -f " + SshService.quote(file) + " ] || exit 2\nstat -c %s "
            + SshService.quote(file) + "\n", List.of(), true, SIMPLE_TIMEOUT);
        if (!result.succeeded()) {
            throw new IllegalArgumentException("Arquivo não encontrado: " + file);
        }
        return parseLong(result.output());
    }

    public void download(Long deviceId, String path, OutputStream sink) {
        Device device = deviceService.requireReady(deviceId);
        String file = normalize(path);
        String script = "[ -f " + SshService.quote(file) + " ] || { echo 'Arquivo não encontrado' >&2; exit 2; }\n"
            + "cat " + SshService.quote(file) + "\n";
        CommandResult result = sshService.download(device, script, true, sink, channel -> {
        });
        if (!result.succeeded()) {
            throw new IllegalArgumentException(firstLine(result.errorOutput(), "Não foi possível baixar " + file));
        }
    }

    public FileUploadResponse upload(Long deviceId, String directory, MultipartFile file) {
        Device device = deviceService.requireReady(deviceId);
        String fileName = file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename();
        if (fileName.contains("/") || fileName.contains("\\") || fileName.equals("..")) {
            throw new IllegalArgumentException("Nome de arquivo inválido.");
        }
        String target = normalize(directory);
        String fullPath = target.endsWith("/") ? target + fileName : target + "/" + fileName;
        try (InputStream input = file.getInputStream()) {
            CommandResult result = sshService.upload(device, "cat > " + SshService.quote(fullPath), true, input, channel -> {
            });
            if (!result.succeeded()) {
                throw new IllegalArgumentException(firstLine(result.errorOutput(), "Não foi possível gravar " + fullPath));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler o arquivo enviado.", exception);
        }
        return new FileUploadResponse(fullPath, file.getSize());
    }

    public void createFolder(Long deviceId, String path) {
        runSimple(deviceId, "mkdir -p " + SshService.quote(normalize(path)), "Não foi possível criar a pasta");
    }

    /** Removes a file or an empty folder. Non-empty folders are refused on purpose. */
    public void delete(Long deviceId, String path) {
        String target = normalize(path);
        if ("/".equals(target)) {
            throw new IllegalArgumentException("A raiz não pode ser apagada.");
        }
        String quoted = SshService.quote(target);
        runSimple(deviceId, "if [ -d " + quoted + " ] && [ ! -L " + quoted + " ]; then rmdir " + quoted
            + " 2>/dev/null || { echo 'A pasta não está vazia. Esvazie antes de apagar.' >&2; exit 1; }; else rm -f " + quoted + "; fi",
            "Não foi possível apagar");
    }

    private void runSimple(Long deviceId, String script, String failure) {
        Device device = deviceService.requireReady(deviceId);
        CommandResult result = sshService.run(device, script, List.of(), true, SIMPLE_TIMEOUT);
        if (!result.succeeded()) {
            throw new IllegalArgumentException(firstLine(result.errorOutput(), failure));
        }
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String trimmed = path.trim().replace('\\', '/');
        if (!trimmed.startsWith("/")) {
            throw new IllegalArgumentException("Use um caminho absoluto, começando por /.");
        }
        return trimmed.length() > 1 && trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String firstLine(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.strip().split("\n")[0];
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}
