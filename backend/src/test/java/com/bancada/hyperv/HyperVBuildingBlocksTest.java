package com.bancada.hyperv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HyperVBuildingBlocksTest {

    private static final int CLUSTER_BITS = 16;
    private static final int CLUSTER = 1 << CLUSTER_BITS;

    @TempDir
    Path folder;

    @Test
    void qcow2WithCompressedPlainAndEmptyClustersBecomesTheSameRawDiskPlusAVhdFooter() throws IOException {
        byte[] first = pattern('A');
        byte[] second = pattern('B');
        long virtualSize = 3L * CLUSTER + 1000; // not a whole megabyte: the VHD is rounded up
        Path qcow2 = folder.resolve("disco.qcow2");
        Files.write(qcow2, qcow2(virtualSize, first, second));

        Path vhd = folder.resolve("disco.vhd");
        long diskSize = Qcow2Image.toFixedVhd(qcow2, vhd, bytes -> { });

        assertEquals(1L << 20, diskSize);
        byte[] disk = Files.readAllBytes(vhd);
        assertEquals(diskSize + 512, disk.length);
        assertArrayEquals(first, Arrays.copyOfRange(disk, 0, CLUSTER), "compressed cluster");
        assertArrayEquals(new byte[CLUSTER], Arrays.copyOfRange(disk, CLUSTER, 2 * CLUSTER), "unallocated cluster reads zeros");
        assertArrayEquals(second, Arrays.copyOfRange(disk, 2 * CLUSTER, 3 * CLUSTER), "plain cluster");
        assertEquals("conectix", new String(disk, (int) diskSize, 8, StandardCharsets.US_ASCII));
        assertTrue(Qcow2Image.isQcow2(qcow2));
        assertTrue(!Qcow2Image.isQcow2(vhd));
    }

    @Test
    void notAQcow2IsRefused() throws IOException {
        Path file = folder.resolve("x.img");
        Files.write(file, new byte[4096]);
        assertThrows(IOException.class, () -> Qcow2Image.toFixedVhd(file, folder.resolve("x.vhd"), bytes -> { }));
    }

    @Test
    void vhdFooterChecksumAndGeometryFollowTheSpecification() {
        byte[] footer = VhdFooter.fixed(10L << 30);
        ByteBuffer buffer = ByteBuffer.wrap(footer);
        int stored = buffer.getInt(64);
        int sum = 0;
        for (int index = 0; index < footer.length; index++) {
            if (index < 64 || index >= 68) {
                sum += footer[index] & 0xff;
            }
        }
        assertEquals(~sum, stored);
        assertEquals(10L << 30, buffer.getLong(48));
        assertEquals(2, buffer.getInt(60), "fixed disk");
        // 10 GB: 20805 cylinders, 16 heads, 63 sectors
        assertEquals((20805 << 16) | (16 << 8) | 63, VhdFooter.geometry(10L << 30));
    }

    @Test
    void checksumFilesOfBothFormatsAreRead() {
        CloudImage ubuntu = CloudImage.sha256("https://x/ubuntu-24.04-server-cloudimg-amd64.img", "https://x/SHA256SUMS");
        assertEquals("abc123", ubuntu.expectedHash("fff *ubuntu-24.04-server-cloudimg-amd64-disk-kvm.img\nABC123 *ubuntu-24.04-server-cloudimg-amd64.img\n"));
        CloudImage rocky = CloudImage.sha256("https://x/Rocky-9-GenericCloud-Base.latest.x86_64.qcow2", "https://x/CHECKSUM");
        assertEquals("92c2", rocky.expectedHash("# Rocky: 1 bytes\nSHA256 (Rocky-9-GenericCloud-Base.latest.x86_64.qcow2) = 92c2\n"));
        assertNull(rocky.expectedHash("SHA256 (outro.qcow2) = 1111\n"));
    }

    @Test
    void cloudInitCarriesUserKeysAndAFixedAddress() {
        CloudInit seed = new CloudInit("web", "bancada-web-1", "admin", "it's s3cret", "sudo", "ssh-rsa PANEL bancada",
            "-----BEGIN RSA PRIVATE KEY-----\nAAA\nBBB\n-----END RSA PRIVATE KEY-----", "ssh-rsa HOST root@web",
            "00:15:5D:4D:00:0A", "10.77.0.10", 24, "10.77.0.1", List.of("1.1.1.1"));
        Map<String, String> files = seed.files();

        assertEquals("instance-id: bancada-web-1\nlocal-hostname: web\n", files.get("meta-data"));
        String user = files.get("user-data");
        assertTrue(user.startsWith("#cloud-config\n"));
        assertTrue(user.contains("      password: 'it''s s3cret'"), "single quotes doubled for YAML");
        assertTrue(user.contains("    groups: [sudo]"));
        assertTrue(user.contains("  rsa_private: |\n    -----BEGIN RSA PRIVATE KEY-----\n    AAA\n    BBB\n"));
        assertTrue(user.contains("ssh_genkeytypes: []"), "no other host key than the pinned one");
        assertTrue(user.contains("      ssh-rsa PANEL bancada\n"));
        String network = files.get("network-config");
        assertTrue(network.contains("macaddress: '00:15:5d:4d:00:0a'"));
        assertTrue(network.contains("addresses: [10.77.0.10/24]"));
        assertTrue(network.contains("via: 10.77.0.1"));
        assertThrows(IllegalArgumentException.class, () -> new CloudInit("web", "i", "admin", "a\nb", "sudo", "k", "p", "h",
            "00:15:5D:4D:00:0A", "10.77.0.10", 24, "10.77.0.1", List.of()));
    }

    @Test
    void powerShellLiteralsCannotBreakOut() {
        assertEquals("'D:\\BancadaVMs\\o''neil'", PowerShell.literal("D:\\BancadaVMs\\o'neil"));
        assertEquals("'$(rm -r C:\\)'", PowerShell.literal("$(rm -r C:\\)"), "single quotes expand nothing");
        assertEquals("$null", PowerShell.literal(null));
    }

    @Test
    void generatedKeysComeBackTheSameFromTheirPrivateHalves() {
        MachineKeys keys = MachineKeys.generate("web");
        MachineKeys restored = MachineKeys.restore(keys.hostPrivateKey(), keys.panelPrivateKey(), "web");
        assertEquals(keys.hostFingerprint(), restored.hostFingerprint());
        assertEquals(keys.panelPublicKey(), restored.panelPublicKey());
        assertTrue(keys.hostPublicKey().startsWith("ssh-rsa "));
        assertTrue(keys.hostFingerprint().startsWith("SHA256:"));
    }

    private static byte[] pattern(char letter) {
        byte[] data = new byte[CLUSTER];
        for (int index = 0; index < data.length; index++) {
            data[index] = (byte) (letter + index % 7);
        }
        return data;
    }

    /** Minimal qcow2 v3: header, L1, one L2, a compressed cluster, a hole, a plain cluster. */
    private static byte[] qcow2(long size, byte[] compressedCluster, byte[] plainCluster) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        deflater.setInput(compressedCluster);
        deflater.finish();
        byte[] compressed = new byte[CLUSTER];
        int compressedLength = deflater.deflate(compressed);
        deflater.end();

        long l1Offset = CLUSTER;
        long l2Offset = 2L * CLUSTER;
        long compressedOffset = 3L * CLUSTER + 100; // compressed data needs no alignment
        long plainOffset = 5L * CLUSTER;
        ByteBuffer image = ByteBuffer.allocate(6 * CLUSTER);
        image.putInt(0, 0x514649fb).putInt(4, 3).putInt(20, CLUSTER_BITS).putLong(24, size)
            .putInt(36, 1).putLong(40, l1Offset).putInt(96, 4).putInt(100, 104);
        image.putLong((int) l1Offset, l2Offset | (1L << 63));
        int shift = 62 - (CLUSTER_BITS - 8);
        long sectors = (compressedOffset % 512 + compressedLength + 511) / 512;
        image.putLong((int) l2Offset, (1L << 62) | ((sectors - 1) << shift) | compressedOffset);
        image.putLong((int) l2Offset + 16, plainOffset | (1L << 63));
        image.put((int) compressedOffset, compressed, 0, compressedLength);
        image.put((int) plainOffset, plainCluster);
        return image.array();
    }
}
