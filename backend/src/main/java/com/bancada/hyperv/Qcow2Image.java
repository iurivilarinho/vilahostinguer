package com.bancada.hyperv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.LongConsumer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Reads a qcow2 image (the format the distributions publish their cloud images in) and writes the
 * disk it holds as a fixed VHD: the raw disk followed by the VHD footer. The output is sparse, only
 * the clusters with data are written, so a 10 GB disk with 1 GB of system takes about 1 GB. Hyper-V
 * then turns it into a VHDX with {@code Convert-VHD}.
 *
 * <p>Supported: versions 2 and 3, zlib compression, no backing file, no encryption, no extended L2
 * (the images published by Ubuntu, Debian, Rocky and AlmaLinux).
 */
public final class Qcow2Image {

    private static final int MAGIC = 0x514649fb;
    private static final long OFFSET_MASK = 0x00fffffffffffe00L;
    private static final long COMPRESSED_FLAG = 1L << 62;
    private static final long ZERO_FLAG = 1L;
    private static final long MEGABYTE = 1L << 20;
    private static final long INCOMPATIBLE_EXTERNAL_DATA = 1L << 2;
    private static final long INCOMPATIBLE_COMPRESSION_TYPE = 1L << 3;
    private static final long INCOMPATIBLE_EXTENDED_L2 = 1L << 4;

    private Qcow2Image() {
    }

    /**
     * Converts {@code source} into a fixed VHD at {@code target}; returns the disk size. The size is
     * rounded up to a whole megabyte, which Hyper-V asks for. {@code progress} receives the guest
     * bytes already processed.
     */
    public static long toFixedVhd(Path source, Path target, LongConsumer progress) throws IOException {
        try (FileChannel in = FileChannel.open(source, StandardOpenOption.READ);
             FileChannel out = FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                 StandardOpenOption.SPARSE)) {
            ByteBuffer header = read(in, 0, 112);
            if (header.getInt(0) != MAGIC) {
                throw new IOException("Não é uma imagem qcow2.");
            }
            int version = header.getInt(4);
            if (version != 2 && version != 3) {
                throw new IOException("Versão de qcow2 não suportada: " + version);
            }
            if (header.getLong(8) != 0) {
                throw new IOException("Imagens qcow2 com arquivo base não são suportadas.");
            }
            int clusterBits = header.getInt(20);
            long size = header.getLong(24);
            if (header.getInt(32) != 0) {
                throw new IOException("Imagens qcow2 cifradas não são suportadas.");
            }
            int l1Size = header.getInt(36);
            long l1Offset = header.getLong(40);
            if (version == 3) {
                long incompatible = header.getLong(72);
                int headerLength = header.getInt(100);
                if ((incompatible & (INCOMPATIBLE_EXTERNAL_DATA | INCOMPATIBLE_EXTENDED_L2)) != 0) {
                    throw new IOException("Recursos de qcow2 não suportados (arquivo de dados externo ou L2 estendido).");
                }
                if ((incompatible & INCOMPATIBLE_COMPRESSION_TYPE) != 0 && headerLength > 104 && header.get(104) != 0) {
                    throw new IOException("Só a compressão zlib é suportada.");
                }
            }
            int clusterSize = 1 << clusterBits;
            int entriesPerL2 = clusterSize / 8;
            int compressedShift = 62 - (clusterBits - 8);
            long compressedOffsetMask = (1L << compressedShift) - 1;
            long compressedSectorsMask = (1L << (clusterBits - 8)) - 1;

            ByteBuffer l1 = read(in, l1Offset, l1Size * 8);
            ByteBuffer cluster = ByteBuffer.allocate(clusterSize);
            Inflater inflater = new Inflater(true);
            try {
                for (int l1Index = 0; l1Index < l1Size; l1Index++) {
                    long l2Offset = l1.getLong(l1Index * 8) & OFFSET_MASK;
                    long firstGuestOffset = (long) l1Index * entriesPerL2 * clusterSize;
                    if (firstGuestOffset >= size) {
                        break;
                    }
                    if (l2Offset == 0) {
                        continue;
                    }
                    ByteBuffer l2 = read(in, l2Offset, clusterSize);
                    for (int l2Index = 0; l2Index < entriesPerL2; l2Index++) {
                        long guestOffset = firstGuestOffset + (long) l2Index * clusterSize;
                        if (guestOffset >= size) {
                            break;
                        }
                        long entry = l2.getLong(l2Index * 8);
                        if ((entry & COMPRESSED_FLAG) != 0) {
                            long hostOffset = entry & compressedOffsetMask;
                            long sectors = ((entry >>> compressedShift) & compressedSectorsMask) + 1;
                            int compressedLength = (int) (sectors * 512 - (hostOffset & 511));
                            inflate(inflater, read(in, hostOffset, (int) Math.min(compressedLength, in.size() - hostOffset)), cluster);
                        } else {
                            long hostOffset = entry & OFFSET_MASK;
                            if (hostOffset == 0 || (entry & ZERO_FLAG) != 0) {
                                continue;
                            }
                            cluster.clear();
                            readFully(in, cluster, hostOffset);
                        }
                        int length = (int) Math.min(clusterSize, size - guestOffset);
                        if (!isZero(cluster, length)) {
                            cluster.position(0).limit(length);
                            while (cluster.hasRemaining()) {
                                out.write(cluster, guestOffset + cluster.position());
                            }
                        }
                    }
                    progress.accept(Math.min(size, firstGuestOffset + (long) entriesPerL2 * clusterSize));
                }
            } finally {
                inflater.end();
            }
            long diskSize = (size + MEGABYTE - 1) / MEGABYTE * MEGABYTE;
            out.write(ByteBuffer.wrap(VhdFooter.fixed(diskSize)), diskSize);
            return diskSize;
        }
    }

    private static void inflate(Inflater inflater, ByteBuffer compressed, ByteBuffer cluster) throws IOException {
        inflater.reset();
        inflater.setInput(compressed.array(), 0, compressed.limit());
        cluster.clear();
        try {
            int total = 0;
            while (total < cluster.capacity() && !inflater.finished()) {
                int count = inflater.inflate(cluster.array(), total, cluster.capacity() - total);
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                total += count;
            }
            if (total < cluster.capacity()) {
                throw new IOException("Bloco comprimido incompleto na imagem.");
            }
        } catch (DataFormatException exception) {
            throw new IOException("Bloco comprimido inválido na imagem: " + exception.getMessage(), exception);
        }
    }

    private static boolean isZero(ByteBuffer buffer, int length) {
        byte[] array = buffer.array();
        for (int index = 0; index < length; index++) {
            if (array[index] != 0) {
                return false;
            }
        }
        return true;
    }

    private static ByteBuffer read(FileChannel channel, long offset, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        readFully(channel, buffer, offset);
        return buffer.flip();
    }

    private static void readFully(FileChannel channel, ByteBuffer buffer, long offset) throws IOException {
        while (buffer.hasRemaining()) {
            int count = channel.read(buffer, offset + buffer.position());
            if (count < 0) {
                throw new IOException("A imagem terminou antes do esperado.");
            }
        }
    }

    /** True when the file starts with the qcow2 magic. */
    public static boolean isQcow2(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
            return channel.size() >= 4 && read(channel, 0, 4).getInt(0) == MAGIC;
        } catch (IOException exception) {
            if (!Files.exists(file)) {
                return false;
            }
            throw exception;
        }
    }
}
