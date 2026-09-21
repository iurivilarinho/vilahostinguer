package com.bancada.hyperv;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * The 512-byte footer that turns a raw disk into a fixed VHD (Microsoft Virtual Hard Disk Image
 * Format Specification, "Hard Disk Footer Format").
 */
public final class VhdFooter {

    private static final long VHD_EPOCH = Instant.parse("2000-01-01T00:00:00Z").getEpochSecond();
    private static final int CHECKSUM_OFFSET = 64;
    private static final SecureRandom RANDOM = new SecureRandom();

    private VhdFooter() {
    }

    public static byte[] fixed(long diskSize) {
        ByteBuffer footer = ByteBuffer.allocate(512);
        footer.put("conectix".getBytes(StandardCharsets.US_ASCII));
        footer.putInt(2);                                  // features: reserved bit always set
        footer.putInt(0x00010000);                         // format version 1.0
        footer.putLong(-1L);                               // data offset: none for a fixed disk
        footer.putInt((int) (Instant.now().getEpochSecond() - VHD_EPOCH));
        footer.put("bnc ".getBytes(StandardCharsets.US_ASCII));
        footer.putInt(0x00010000);                         // creator version
        footer.put("Wi2k".getBytes(StandardCharsets.US_ASCII));
        footer.putLong(diskSize);                          // original size
        footer.putLong(diskSize);                          // current size
        footer.putInt(geometry(diskSize));
        footer.putInt(2);                                  // disk type: fixed
        footer.putInt(0);                                  // checksum, filled below
        byte[] uniqueId = new byte[16];
        RANDOM.nextBytes(uniqueId);
        footer.put(uniqueId);
        byte[] bytes = footer.array();
        int sum = 0;
        for (byte value : bytes) {
            sum += value & 0xff;
        }
        ByteBuffer.wrap(bytes).putInt(CHECKSUM_OFFSET, ~sum);
        return bytes;
    }

    /** Cylinders (16 bits), heads (8) and sectors per track (8), as the specification computes them. */
    static int geometry(long diskSize) {
        long totalSectors = Math.min(diskSize / 512, 65535L * 16 * 255);
        long sectorsPerTrack;
        long heads;
        long cylinderTimesHeads;
        if (totalSectors >= 65535L * 16 * 63) {
            sectorsPerTrack = 255;
            heads = 16;
            cylinderTimesHeads = totalSectors / sectorsPerTrack;
        } else {
            sectorsPerTrack = 17;
            cylinderTimesHeads = totalSectors / sectorsPerTrack;
            heads = Math.max(4, (cylinderTimesHeads + 1023) / 1024);
            if (cylinderTimesHeads >= heads * 1024 || heads > 16) {
                sectorsPerTrack = 31;
                heads = 16;
                cylinderTimesHeads = totalSectors / sectorsPerTrack;
            }
            if (cylinderTimesHeads >= heads * 1024) {
                sectorsPerTrack = 63;
                heads = 16;
                cylinderTimesHeads = totalSectors / sectorsPerTrack;
            }
        }
        long cylinders = cylinderTimesHeads / heads;
        return (int) ((cylinders << 16) | (heads << 8) | sectorsPerTrack);
    }
}
