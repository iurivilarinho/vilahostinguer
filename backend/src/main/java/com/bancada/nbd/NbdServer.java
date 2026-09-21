package com.bancada.nbd;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network Block Device server (fixed newstyle handshake, simple replies): the devices attach the
 * virtual disks kept on this PC with {@code nbd-client} and see them as ordinary disks.
 *
 * <p>NBD has no authentication. An export name is a long random secret, and a connection is only
 * served when it comes from the address of the device the disk was given to.
 */
public class NbdServer implements AutoCloseable {

    /** Which disk a client asks for, if that client may have it. */
    public interface ExportResolver {
        Export resolve(String name, InetAddress client);
    }

    /** Told when a disk gets or loses its device. */
    public interface ConnectionListener {
        void connected(Long volumeId, String client);

        void disconnected(Long volumeId, String reason);
    }

    public record Export(Long volumeId, Path file) {
    }

    private static final Logger LOG = LoggerFactory.getLogger(NbdServer.class);

    private static final long NBDMAGIC = 0x4e42444d41474943L;
    private static final long IHAVEOPT = 0x49484156454F5054L;
    private static final long OPTION_REPLY_MAGIC = 0x3e889045565a9L;
    private static final int REQUEST_MAGIC = 0x25609513;
    private static final int SIMPLE_REPLY_MAGIC = 0x67446698;

    private static final int FLAG_FIXED_NEWSTYLE = 1;
    private static final int FLAG_NO_ZEROES = 2;

    private static final int OPT_EXPORT_NAME = 1;
    private static final int OPT_ABORT = 2;
    private static final int OPT_LIST = 3;
    private static final int OPT_INFO = 6;
    private static final int OPT_GO = 7;

    private static final int REP_ACK = 1;
    private static final int REP_INFO = 3;
    private static final int REP_ERR_UNSUP = 0x80000001;
    private static final int REP_ERR_POLICY = 0x80000002;
    private static final int REP_ERR_INVALID = 0x80000003;
    private static final int REP_ERR_UNKNOWN = 0x80000006;
    private static final short INFO_EXPORT = 0;

    private static final short TRANSMISSION_FLAGS = 1 | 4 | 8; // HAS_FLAGS, SEND_FLUSH, SEND_FUA

    private static final int CMD_READ = 0;
    private static final int CMD_WRITE = 1;
    private static final int CMD_DISC = 2;
    private static final int CMD_FLUSH = 3;
    private static final int CMD_FLAG_FUA = 1;

    private static final int EIO = 5;
    private static final int EINVAL = 22;
    private static final int ENOSPC = 28;

    private static final int MAX_OPTION_BYTES = 4096;
    private static final int MAX_REQUEST_BYTES = 32 << 20;
    private static final int HANDSHAKE_TIMEOUT_MS = 15_000;
    private static final int STREAM_BUFFER = 256 * 1024;

    private final ExportResolver resolver;
    private final ConnectionListener listener;
    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final Map<Long, Socket> active = new ConcurrentHashMap<>();
    private final Map<Long, Object> imageLocks = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public NbdServer(InetSocketAddress address, ExportResolver resolver, ConnectionListener listener) throws IOException {
        this.resolver = resolver;
        this.listener = listener;
        this.serverSocket = new ServerSocket();
        this.serverSocket.setReuseAddress(true);
        this.serverSocket.bind(address);
        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable, "nbd-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        executor.execute(this::acceptLoop);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public boolean isConnected(Long volumeId) {
        return active.containsKey(volumeId);
    }

    /** Drops the connection of a disk (it is being removed or given to another device). */
    public void disconnect(Long volumeId) {
        Socket socket = active.remove(volumeId);
        closeQuietly(socket);
    }

    @Override
    public void close() {
        closed = true;
        closeQuietly(serverSocket);
        active.values().forEach(NbdServer::closeQuietly);
        executor.shutdownNow();
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = serverSocket.accept();
                executor.execute(() -> serve(socket));
            } catch (IOException exception) {
                if (!closed) {
                    LOG.warn("NBD accept failed: {}", exception.getMessage());
                }
            }
        }
    }

    private void serve(Socket socket) {
        Export export = null;
        String reason = "conexão encerrada";
        try (socket) {
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), STREAM_BUFFER));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), STREAM_BUFFER));
            export = negotiate(socket, in, out);
            if (export == null) {
                return;
            }
            socket.setSoTimeout(0);
            Socket previous = active.put(export.volumeId(), socket);
            // a device that reconnects (nbd-client -persist) replaces its own half-dead connection
            closeQuietly(previous);
            listener.connected(export.volumeId(), socket.getInetAddress().getHostAddress());
            // the replaced connection closes its image (and saves its map) before this one opens it
            synchronized (imageLocks.computeIfAbsent(export.volumeId(), id -> new Object())) {
                try (VolumeImage image = VolumeImage.open(export.file())) {
                    reason = transmit(image, in, out);
                }
            }
        } catch (EOFException | SocketException exception) {
            reason = "a conexão caiu";
        } catch (IOException | RuntimeException exception) {
            reason = exception.getMessage();
            LOG.warn("NBD connection failed: {}", exception.getMessage());
        } finally {
            if (export != null && active.remove(export.volumeId(), socket)) {
                listener.disconnected(export.volumeId(), reason);
            }
        }
    }

    /** Option haggling; returns the export to serve, or null when the client left or was refused. */
    private Export negotiate(Socket socket, DataInputStream in, DataOutputStream out) throws IOException {
        out.writeLong(NBDMAGIC);
        out.writeLong(IHAVEOPT);
        out.writeShort(FLAG_FIXED_NEWSTYLE | FLAG_NO_ZEROES);
        out.flush();
        int clientFlags = in.readInt();
        boolean noZeroes = (clientFlags & FLAG_NO_ZEROES) != 0;
        InetAddress client = socket.getInetAddress();
        while (true) {
            if (in.readLong() != IHAVEOPT) {
                return null;
            }
            int option = in.readInt();
            int length = in.readInt();
            if (length < 0 || length > MAX_OPTION_BYTES) {
                return null;
            }
            byte[] data = new byte[length];
            in.readFully(data);
            switch (option) {
                case OPT_EXPORT_NAME -> {
                    Export export = resolver.resolve(new String(data, StandardCharsets.UTF_8), client);
                    if (export == null) {
                        // this old option has no way to say no: the connection just ends
                        return null;
                    }
                    out.writeLong(sizeOf(export));
                    out.writeShort(TRANSMISSION_FLAGS);
                    if (!noZeroes) {
                        out.write(new byte[124]);
                    }
                    out.flush();
                    return export;
                }
                case OPT_INFO, OPT_GO -> {
                    ByteBuffer buffer = ByteBuffer.wrap(data);
                    if (data.length < 6) {
                        optionReply(out, option, REP_ERR_INVALID, new byte[0]);
                        continue;
                    }
                    int nameLength = buffer.getInt();
                    if (nameLength < 0 || nameLength > data.length - 6) {
                        optionReply(out, option, REP_ERR_INVALID, new byte[0]);
                        continue;
                    }
                    byte[] name = new byte[nameLength];
                    buffer.get(name);
                    Export export = resolver.resolve(new String(name, StandardCharsets.UTF_8), client);
                    if (export == null) {
                        optionReply(out, option, REP_ERR_UNKNOWN, new byte[0]);
                        continue;
                    }
                    ByteBuffer info = ByteBuffer.allocate(12);
                    info.putShort(INFO_EXPORT).putLong(sizeOf(export)).putShort(TRANSMISSION_FLAGS);
                    optionReply(out, option, REP_INFO, info.array());
                    optionReply(out, option, REP_ACK, new byte[0]);
                    if (option == OPT_GO) {
                        return export;
                    }
                }
                case OPT_ABORT -> {
                    optionReply(out, option, REP_ACK, new byte[0]);
                    return null;
                }
                // listing would hand out the secret names
                case OPT_LIST -> optionReply(out, option, REP_ERR_POLICY, new byte[0]);
                default -> optionReply(out, option, REP_ERR_UNSUP, new byte[0]);
            }
        }
    }

    private static long sizeOf(Export export) throws IOException {
        return Files.size(export.file());
    }

    private static void optionReply(DataOutputStream out, int option, int type, byte[] data) throws IOException {
        out.writeLong(OPTION_REPLY_MAGIC);
        out.writeInt(option);
        out.writeInt(type);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    private String transmit(VolumeImage image, DataInputStream in, DataOutputStream out) throws IOException {
        byte[] buffer = new byte[128 * 1024];
        while (true) {
            if (in.readInt() != REQUEST_MAGIC) {
                return "pedido inválido do cliente";
            }
            int flags = in.readUnsignedShort();
            int type = in.readUnsignedShort();
            long handle = in.readLong();
            long offset = in.readLong();
            long length = in.readInt() & 0xffffffffL;
            if (type == CMD_DISC) {
                image.flush();
                return "desconectado pelo dispositivo";
            }
            if ((type == CMD_READ || type == CMD_WRITE) && length > MAX_REQUEST_BYTES) {
                return "pedido grande demais";
            }
            boolean inRange = offset >= 0 && offset + length <= image.size();
            if (buffer.length < length) {
                buffer = new byte[(int) length];
            }
            switch (type) {
                case CMD_READ -> {
                    if (!inRange) {
                        reply(out, EINVAL, handle, null, 0);
                        continue;
                    }
                    int error = 0;
                    try {
                        image.read(ByteBuffer.wrap(buffer, 0, (int) length), offset);
                    } catch (IOException exception) {
                        LOG.warn("NBD read failed at {}: {}", offset, exception.getMessage());
                        error = EIO;
                    }
                    reply(out, error, handle, buffer, error == 0 ? (int) length : 0);
                }
                case CMD_WRITE -> {
                    in.readFully(buffer, 0, (int) length);
                    if (!inRange) {
                        reply(out, EINVAL, handle, null, 0);
                        continue;
                    }
                    int error = 0;
                    try {
                        image.write(ByteBuffer.wrap(buffer, 0, (int) length), offset);
                        if ((flags & CMD_FLAG_FUA) != 0) {
                            image.flush();
                        }
                    } catch (IOException exception) {
                        LOG.warn("NBD write failed at {}: {}", offset, exception.getMessage());
                        error = isDiskFull(exception) ? ENOSPC : EIO;
                    }
                    reply(out, error, handle, null, 0);
                }
                case CMD_FLUSH -> {
                    int error = 0;
                    try {
                        image.flush();
                    } catch (IOException exception) {
                        error = EIO;
                    }
                    reply(out, error, handle, null, 0);
                }
                default -> reply(out, EINVAL, handle, null, 0);
            }
        }
    }

    private static void reply(DataOutputStream out, int error, long handle, byte[] data, int length) throws IOException {
        out.writeInt(SIMPLE_REPLY_MAGIC);
        out.writeInt(error);
        out.writeLong(handle);
        if (data != null && length > 0) {
            out.write(data, 0, length);
        }
        out.flush();
    }

    private static boolean isDiskFull(IOException exception) {
        String message = exception.getMessage();
        return message != null && (message.contains("not enough space") || message.contains("espaço suficiente")
            || message.contains("No space"));
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // closing anyway
        }
    }
}
