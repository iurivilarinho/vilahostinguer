package com.bancada.nbd;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;

/**
 * The file on this PC that holds a virtual disk. It is sparse: space on the PC disk is taken as the
 * device writes, not when the disk is created. Next to it a small map records which 1 MB blocks were
 * ever written, so the panel knows how much of the promised size is still to be taken and never
 * promises more than the PC disk has.
 */
public class VolumeImage implements AutoCloseable {

    public static final int BLOCK_BYTES = 1 << 20;
    private static final String MAP_SUFFIX = ".map";

    private final Path file;
    private final Path mapFile;
    private final long size;
    private final FileChannel channel;
    private final BitSet written;
    private boolean mapDirty;

    private VolumeImage(Path file, long size, FileChannel channel, BitSet written) {
        this.file = file;
        this.mapFile = mapOf(file);
        this.size = size;
        this.channel = channel;
        this.written = written;
    }

    /** New sparse file of {@code size} bytes; fails if the file already exists. */
    public static void create(Path file, long size) throws IOException {
        Files.createDirectories(file.getParent());
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
            StandardOpenOption.SPARSE)) {
            // one byte at the end sets the length; with SPARSE nothing before it is allocated
            channel.write(ByteBuffer.wrap(new byte[1]), size - 1);
        }
        Files.write(mapOf(file), new byte[0]);
    }

    public static VolumeImage open(Path file) throws IOException {
        FileChannel channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
        long size = channel.size();
        return new VolumeImage(file, size, channel, readMap(file, size));
    }

    /**
     * Bytes of the PC disk this image already took (an upper bound: the map counts whole blocks). A
     * missing map means it cannot be known, and the whole size counts as taken.
     */
    public static long writtenBytes(Path file, long size) {
        if (!Files.exists(mapOf(file))) {
            return size;
        }
        try {
            return Math.min(size, (long) readMap(file, size).cardinality() * BLOCK_BYTES);
        } catch (IOException exception) {
            return size;
        }
    }

    public static void delete(Path file) throws IOException {
        Files.deleteIfExists(file);
        Files.deleteIfExists(mapOf(file));
    }

    public long size() {
        return size;
    }

    public void read(ByteBuffer target, long offset) throws IOException {
        while (target.hasRemaining()) {
            int count = channel.read(target, offset);
            if (count < 0) {
                // past the written end of a sparse file: zeros
                while (target.hasRemaining()) {
                    target.put((byte) 0);
                }
                return;
            }
            offset += count;
        }
    }

    public synchronized void write(ByteBuffer source, long offset) throws IOException {
        markRange(offset, source.remaining());
        while (source.hasRemaining()) {
            offset += channel.write(source, offset);
        }
    }

    /** Data and map on the PC disk. */
    public synchronized void flush() throws IOException {
        channel.force(false);
        saveMap();
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            saveMap();
        } finally {
            channel.close();
        }
    }

    /** Records the blocks touched by a write of {@code length} bytes at {@code offset}. */
    private void markRange(long offset, long length) {
        if (length <= 0) {
            return;
        }
        int first = (int) (offset / BLOCK_BYTES);
        int last = (int) ((offset + length - 1) / BLOCK_BYTES);
        for (int block = first; block <= last; block++) {
            if (!written.get(block)) {
                written.set(block);
                mapDirty = true;
            }
        }
    }

    private void saveMap() throws IOException {
        if (mapDirty) {
            Path temporary = mapFile.resolveSibling(mapFile.getFileName() + ".tmp");
            Files.write(temporary, written.toByteArray());
            Files.move(temporary, mapFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            mapDirty = false;
        }
    }

    private static BitSet readMap(Path file, long size) throws IOException {
        Path map = mapOf(file);
        if (!Files.exists(map)) {
            BitSet all = new BitSet();
            all.set(0, (int) ((size + BLOCK_BYTES - 1) / BLOCK_BYTES));
            return all;
        }
        return BitSet.valueOf(Files.readAllBytes(map));
    }

    private static Path mapOf(Path file) {
        return file.resolveSibling(file.getFileName() + MAP_SUFFIX);
    }

    public Path file() {
        return file;
    }
}
