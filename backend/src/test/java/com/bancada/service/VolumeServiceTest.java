package com.bancada.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bancada.nbd.VolumeImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VolumeServiceTest {

    @TempDir
    Path folder;

    @Test
    void mountFolderMustBeInsideADataRoot() {
        assertDoesNotThrow(() -> VolumeService.validateMountPath("/mnt/dados"));
        assertDoesNotThrow(() -> VolumeService.validateMountPath("/srv/bancada/discos/site"));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateMountPath("/mnt"));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateMountPath("/etc/dados"));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateMountPath("/mnt/../etc"));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateMountPath("/var/lib/docker"));
    }

    @Test
    void machineFolderCannotHideTheSystem() {
        assertDoesNotThrow(() -> VolumeService.validateContainerPath("/dados"));
        assertDoesNotThrow(() -> VolumeService.validateContainerPath("/var/lib/mysql"));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateContainerPath(null));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateContainerPath("/"));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateContainerPath("/usr"));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateContainerPath("/etc/nginx"));
        assertThrows(IllegalArgumentException.class, () -> VolumeService.validateContainerPath("/proc/x"));
    }

    @Test
    void imageIsSparseAndCountsWrittenBlocks() throws IOException {
        Path file = folder.resolve("d.img");
        long size = 64L << 20;
        VolumeImage.create(file, size);
        assertEquals(size, Files.size(file));
        assertEquals(0, VolumeImage.writtenBytes(file, size));

        try (VolumeImage image = VolumeImage.open(file)) {
            // straddles the first two blocks
            image.write(ByteBuffer.wrap(new byte[4096]), VolumeImage.BLOCK_BYTES - 1024);
            image.write(ByteBuffer.wrap(new byte[10]), 40L << 20);
        }
        assertEquals(3L * VolumeImage.BLOCK_BYTES, VolumeImage.writtenBytes(file, size));
    }

    @Test
    void lostMapCountsTheWholeDiskAsTaken() throws IOException {
        Path file = folder.resolve("d.img");
        VolumeImage.create(file, 8L << 20);
        Files.delete(folder.resolve("d.img.map"));
        assertEquals(8L << 20, VolumeImage.writtenBytes(file, 8L << 20));
        VolumeImage.delete(file);
        assertTrue(Files.notExists(file));
    }
}
