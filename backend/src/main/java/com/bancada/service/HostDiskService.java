package com.bancada.service;

import com.bancada.enums.VolumeStatus;
import com.bancada.models.Volume;
import com.bancada.nbd.VolumeImage;
import com.bancada.repository.VolumeRepository;
import com.bancada.response.HostDiskResponse;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * The disks of this PC and how much of each the virtual disks already promised. A virtual disk is a
 * sparse file: it takes space as the device writes. What is still to be written stays reserved, so
 * the sum of the promises never passes the free space and the device never meets a full PC disk.
 */
@Service
public class HostDiskService {

    /** Left free on every PC disk for Windows and everything else. */
    public static final long MARGIN_BYTES = 2L << 30;
    public static final String FOLDER = "BancadaDiscos";
    private static final Set<String> SPARSE_FILE_SYSTEMS = Set.of("NTFS", "REFS");

    private final VolumeRepository volumeRepository;

    public HostDiskService(VolumeRepository volumeRepository) {
        this.volumeRepository = volumeRepository;
    }

    public List<HostDiskResponse> list() {
        List<Volume> volumes = volumeRepository.findByStatusNot(VolumeStatus.DELETED);
        List<HostDiskResponse> disks = new ArrayList<>();
        for (Path root : FileSystems.getDefault().getRootDirectories()) {
            HostDiskResponse disk = describe(root, volumes);
            if (disk != null) {
                disks.add(disk);
            }
        }
        return disks;
    }

    /** The disk with this root, if it can hold a new virtual disk of {@code sizeBytes}. */
    public HostDiskResponse requireRoom(String drive, long sizeBytes) {
        HostDiskResponse disk = list().stream()
            .filter(candidate -> candidate.root().equalsIgnoreCase(drive.trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Disco do PC não encontrado: " + drive));
        if (!disk.supported()) {
            throw new IllegalArgumentException("O disco " + disk.root() + " não pode guardar discos virtuais: " + disk.unsupportedReason());
        }
        if (sizeBytes > disk.availableBytes()) {
            throw new IllegalStateException("Não cabe em " + disk.root() + ": sobram " + gigabytes(disk.availableBytes())
                + " para discos novos (livre menos o que os outros discos ainda vão ocupar e uma folga de "
                + gigabytes(MARGIN_BYTES) + ").");
        }
        return disk;
    }

    public static Path folderOf(String root) {
        return Paths.get(root, FOLDER);
    }

    private HostDiskResponse describe(Path root, List<Volume> volumes) {
        FileStore store;
        long total;
        long free;
        try {
            store = Files.getFileStore(root);
            if (Boolean.TRUE.equals(attribute(store, "volume:isCdrom"))) {
                return null;
            }
            total = store.getTotalSpace();
            free = store.getUsableSpace();
        } catch (IOException | SecurityException exception) {
            // drive without media (card reader) or not ready
            return null;
        }
        String rootText = root.toString();
        long allocated = 0;
        long reserved = 0;
        for (Volume volume : volumes) {
            if (volume.getDrive().equalsIgnoreCase(rootText)) {
                allocated += volume.getSizeBytes();
                reserved += volume.getSizeBytes() - VolumeImage.writtenBytes(Paths.get(volume.getFilePath()), volume.getSizeBytes());
            }
        }
        String fileSystem = store.type();
        boolean removable = Boolean.TRUE.equals(attribute(store, "volume:isRemovable"));
        String reason = null;
        if (!SPARSE_FILE_SYSTEMS.contains(fileSystem.toUpperCase(Locale.ROOT))) {
            reason = fileSystem.toUpperCase(Locale.ROOT).startsWith("FAT")
                ? "FAT32 não guarda arquivos maiores que 4 GB. Formate como NTFS."
                : fileSystem + " não tem arquivos esparsos. Use um disco NTFS.";
        }
        long available = Math.max(0, free - reserved - MARGIN_BYTES);
        return new HostDiskResponse(rootText, store.name(), fileSystem, total, free, allocated, reserved, reason == null ? available : 0,
            removable, reason == null, reason);
    }

    private static Object attribute(FileStore store, String name) {
        try {
            return store.getAttribute(name);
        } catch (IOException | UnsupportedOperationException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static String gigabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f GB", bytes / (double) (1L << 30));
    }
}
