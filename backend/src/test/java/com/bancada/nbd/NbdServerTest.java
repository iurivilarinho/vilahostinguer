package com.bancada.nbd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NbdServerTest {

    private static final long IHAVEOPT = 0x49484156454F5054L;
    private static final String SECRET = "0123456789abcdef";
    private static final long SIZE = 8L << 20;

    @TempDir
    Path folder;

    private Path file;
    private NbdServer server;
    private final List<String> events = new CopyOnWriteArrayList<>();
    private volatile boolean refuseEveryone;

    @BeforeEach
    void setUp() throws IOException {
        file = folder.resolve("disco.img");
        VolumeImage.create(file, SIZE);
        server = new NbdServer(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
            (name, client) -> !refuseEveryone && SECRET.equals(name) ? new NbdServer.Export(7L, file) : null,
            new NbdServer.ConnectionListener() {
                @Override
                public void connected(Long volumeId, String client) {
                    events.add("connected " + volumeId);
                }

                @Override
                public void disconnected(Long volumeId, String reason) {
                    events.add("disconnected " + volumeId);
                }
            });
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void goNegotiationThenWriteReadFlushAndDisconnect() throws Exception {
        try (Socket socket = connect()) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            greet(in, out);

            sendGo(out, SECRET);
            // NBD_REP_INFO with NBD_INFO_EXPORT: size and flags
            assertEquals(3, readOptionReply(in, 7));
            assertEquals(12, in.readInt());
            assertEquals(0, in.readShort());
            assertEquals(SIZE, in.readLong());
            short flags = in.readShort();
            assertTrue((flags & 4) != 0, "announces flush");
            assertEquals(1, readOptionReply(in, 7));
            assertEquals(0, in.readInt());

            byte[] data = "dados do dispositivo".getBytes(StandardCharsets.UTF_8);
            request(out, 1, 11L, 3L << 20, data.length);
            out.write(data);
            assertReply(in, 0, 11L);

            request(out, 0, 12L, 3L << 20, data.length);
            assertReply(in, 0, 12L);
            byte[] back = new byte[data.length];
            in.readFully(back);
            assertArrayEquals(data, back);

            // never written: zeros
            request(out, 0, 13L, 5L << 20, 4);
            assertReply(in, 0, 13L);
            byte[] zeros = new byte[4];
            in.readFully(zeros);
            assertArrayEquals(new byte[4], zeros);

            request(out, 3, 14L, 0, 0);
            assertReply(in, 0, 14L);

            // past the end: EINVAL, and the connection goes on
            request(out, 0, 15L, SIZE - 2, 4);
            assertReply(in, 22, 15L);

            request(out, 2, 16L, 0, 0);
            waitFor(() -> events.contains("disconnected 7"));
        }
        assertEquals(List.of("connected 7", "disconnected 7"), events);
        assertEquals(1L << 20, VolumeImage.writtenBytes(file, SIZE), "one 1 MB block written");
    }

    @Test
    void exportNameNegotiationForOldClients() throws Exception {
        try (Socket socket = connect()) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            greet(in, out);
            byte[] name = SECRET.getBytes(StandardCharsets.UTF_8);
            out.writeLong(IHAVEOPT);
            out.writeInt(1);
            out.writeInt(name.length);
            out.write(name);
            assertEquals(SIZE, in.readLong());
            in.readShort();
            // the client asked for no zeroes: transmission starts right away
            request(out, 3, 1L, 0, 0);
            assertReply(in, 0, 1L);
        }
    }

    @Test
    void unknownOrForeignClientIsRefused() throws Exception {
        try (Socket socket = connect()) {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            greet(in, out);
            sendGo(out, "outro-nome");
            assertEquals(0x80000006, readOptionReply(in, 7));
            assertEquals(0, in.readInt());

            refuseEveryone = true;
            sendGo(out, SECRET);
            assertEquals(0x80000006, readOptionReply(in, 7));
            assertEquals(0, in.readInt());

            // listing would give the secret names away
            out.writeLong(IHAVEOPT);
            out.writeInt(3);
            out.writeInt(0);
            assertEquals(0x80000002, readOptionReply(in, 3));
        }
        assertTrue(events.isEmpty());
        assertFalse(server.isConnected(7L));
    }

    @Test
    void reconnectReplacesTheOldConnection() throws Exception {
        Socket first = connect();
        DataInputStream firstIn = new DataInputStream(first.getInputStream());
        DataOutputStream firstOut = new DataOutputStream(first.getOutputStream());
        greet(firstIn, firstOut);
        goAndSkipReplies(firstIn, firstOut);
        waitFor(() -> server.isConnected(7L));

        try (Socket second = connect()) {
            DataInputStream in = new DataInputStream(second.getInputStream());
            DataOutputStream out = new DataOutputStream(second.getOutputStream());
            greet(in, out);
            goAndSkipReplies(in, out);
            request(out, 3, 2L, 0, 0);
            assertReply(in, 0, 2L);
            // the old socket was closed by the server
            boolean closed;
            try {
                closed = firstIn.read() < 0;
            } catch (IOException exception) {
                closed = true;
            }
            assertTrue(closed);
        } finally {
            first.close();
        }
    }

    private Socket connect() throws IOException {
        Socket socket = new Socket(InetAddress.getLoopbackAddress(), server.port());
        socket.setSoTimeout(5000);
        return socket;
    }

    private static void greet(DataInputStream in, DataOutputStream out) throws IOException {
        assertEquals(0x4e42444d41474943L, in.readLong());
        assertEquals(IHAVEOPT, in.readLong());
        assertEquals(3, in.readUnsignedShort());
        out.writeInt(3);
    }

    private static void sendGo(DataOutputStream out, String name) throws IOException {
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        out.writeLong(IHAVEOPT);
        out.writeInt(7);
        out.writeInt(4 + bytes.length + 2);
        out.writeInt(bytes.length);
        out.write(bytes);
        out.writeShort(0);
    }

    private static void goAndSkipReplies(DataInputStream in, DataOutputStream out) throws IOException {
        sendGo(out, SECRET);
        readOptionReply(in, 7);
        in.skipNBytes(in.readInt());
        readOptionReply(in, 7);
        in.readInt();
    }

    /** Reads magic and option, checks them, returns the reply type (the length is read by the caller). */
    private static int readOptionReply(DataInputStream in, int option) throws IOException {
        assertEquals(0x3e889045565a9L, in.readLong());
        assertEquals(option, in.readInt());
        return in.readInt();
    }

    private static void request(DataOutputStream out, int type, long handle, long offset, int length) throws IOException {
        out.writeInt(0x25609513);
        out.writeShort(0);
        out.writeShort(type);
        out.writeLong(handle);
        out.writeLong(offset);
        out.writeInt(length);
    }

    private static void assertReply(DataInputStream in, int error, long handle) throws IOException {
        try {
            assertEquals(0x67446698, in.readInt());
        } catch (EOFException exception) {
            throw new AssertionError("server closed the connection", exception);
        }
        assertEquals(error, in.readInt());
        assertEquals(handle, in.readLong());
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(condition.getAsBoolean());
    }
}
