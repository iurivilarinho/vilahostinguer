package com.bancada.gateway;

import com.bancada.enums.RouteType;
import com.bancada.records.RouteTarget;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.net.ssl.SSLSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One port opened on this PC for remote access. Each connection is relayed byte for byte to the
 * destination picked by the resolver: the Host header (HTTP), the TLS server name (HTTPS) or
 * nothing at all (TCP, one route per port). Two threads per connection: fine for a home server.
 */
public final class GatewayListener implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayListener.class);
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int FIRST_BYTES_TIMEOUT_MS = 15_000;
    private static final int BUFFER_BYTES = 16 * 1024;
    private static final int BACKLOG = 128;

    private final int port;
    private final RouteType type;
    private final ServerSocket server;
    private final ExecutorService executor;
    private final Function<Long, TrafficCounter> traffic;
    private volatile Function<String, RouteTarget> resolver;
    private volatile boolean closed;

    private GatewayListener(int port, RouteType type, ServerSocket server, ExecutorService executor,
                            Function<String, RouteTarget> resolver, Function<Long, TrafficCounter> traffic) {
        this.port = port;
        this.type = type;
        this.server = server;
        this.executor = executor;
        this.resolver = resolver;
        this.traffic = traffic;
    }

    /** Binds on every interface of the PC and starts accepting. */
    public static GatewayListener open(int port, RouteType type, ExecutorService executor,
                                       Function<String, RouteTarget> resolver, Function<Long, TrafficCounter> traffic) throws IOException {
        ServerSocket server = new ServerSocket();
        try {
            server.bind(new InetSocketAddress(port), BACKLOG);
        } catch (IOException exception) {
            server.close();
            throw exception;
        }
        GatewayListener listener = new GatewayListener(port, type, server, executor, resolver, traffic);
        executor.execute(listener::acceptLoop);
        LOG.info("Gateway listening on port {} ({}).", port, type);
        return listener;
    }

    /** Swaps the routing table without closing the port (routes added, removed or retargeted). */
    public void updateResolver(Function<String, RouteTarget> resolver) {
        this.resolver = resolver;
    }

    public int getPort() {
        return port;
    }

    public RouteType getType() {
        return type;
    }

    @Override
    public void close() {
        closed = true;
        closeQuietly(server);
        LOG.info("Gateway port {} closed.", port);
    }

    private void acceptLoop() {
        while (!closed) {
            try {
                Socket client = server.accept();
                executor.execute(() -> handle(client));
            } catch (IOException exception) {
                if (!closed) {
                    LOG.warn("Gateway port {} stopped accepting: {}", port, exception.getMessage());
                }
                return;
            }
        }
    }

    private void handle(Socket client) {
        try {
            client.setSoTimeout(FIRST_BYTES_TIMEOUT_MS);
            switch (type) {
                case TCP -> {
                    RouteTarget target = resolver.apply(null);
                    if (target == null) {
                        closeQuietly(client);
                        return;
                    }
                    relay(client, target, new byte[0], false);
                }
                case HTTP -> {
                    HttpRequestHead head = HttpRequestHead.read(client.getInputStream());
                    if (head == null) {
                        closeQuietly(client);
                        return;
                    }
                    RouteTarget target = head.host() == null ? null : resolver.apply(head.host());
                    if (target == null) {
                        answer(client, HttpRequestHead.page(404, "Not Found", "Nenhum site publicado aqui",
                            head.host() == null ? "O pedido não informou o nome do site."
                                : "Não há rota para " + head.host() + " neste servidor."));
                        return;
                    }
                    relay(client, target, head.forwardedBytes(clientAddress(client), "http", target.singleRequest()), true);
                }
                case TLS -> {
                    TlsClientHello hello = TlsClientHello.read(client.getInputStream());
                    RouteTarget target = hello == null || hello.serverName() == null ? null : resolver.apply(hello.serverName());
                    if (target == null) {
                        closeQuietly(client);
                        return;
                    }
                    if (target.tlsContext() != null) {
                        terminate(client, target, hello);
                    } else {
                        relay(client, target, hello.bytes(), false);
                    }
                }
            }
        } catch (IOException exception) {
            closeQuietly(client);
        } catch (RuntimeException exception) {
            LOG.warn("Gateway port {}: unexpected failure on a connection", port, exception);
            closeQuietly(client);
        }
    }

    /**
     * Ends TLS here with the certificate of the route (the ClientHello already read is handed back to
     * the TLS engine), then relays plain HTTP marked as https to the destination.
     */
    private void terminate(Socket client, RouteTarget target, TlsClientHello hello) throws IOException {
        SSLSocket secure = (SSLSocket) target.tlsContext().getSocketFactory()
            .createSocket(client, new ByteArrayInputStream(hello.bytes()), true);
        secure.setUseClientMode(false);
        secure.setSoTimeout(FIRST_BYTES_TIMEOUT_MS);
        secure.startHandshake();
        HttpRequestHead head = HttpRequestHead.read(secure.getInputStream());
        if (head == null) {
            closeQuietly(secure);
            return;
        }
        relay(secure, target, head.forwardedBytes(clientAddress(client), "https", target.singleRequest()), true);
    }

    private void relay(Socket client, RouteTarget target, byte[] firstBytes, boolean speaksHttp) throws IOException {
        TrafficCounter counter = traffic.apply(target.routeId());
        counter.opened();
        Socket upstream = new Socket();
        try {
            upstream.connect(new InetSocketAddress(target.host(), target.port()), CONNECT_TIMEOUT_MS);
        } catch (IOException exception) {
            counter.error("O destino " + target.host() + ":" + target.port() + " não respondeu (" + exception.getMessage() + ")");
            counter.closed();
            closeQuietly(upstream);
            if (speaksHttp) {
                answer(client, HttpRequestHead.page(502, "Bad Gateway", "O site não respondeu",
                    target.label() + " não aceitou a conexão. Veja se o serviço e o dispositivo estão ligados."));
            } else {
                closeQuietly(client);
            }
            return;
        }
        client.setSoTimeout(0);
        client.setTcpNoDelay(true);
        upstream.setTcpNoDelay(true);
        if (firstBytes.length > 0) {
            upstream.getOutputStream().write(firstBytes);
            counter.addIn(firstBytes.length);
        }
        AtomicInteger openDirections = new AtomicInteger(2);
        Runnable directionDone = () -> {
            if (openDirections.decrementAndGet() == 0) {
                closeQuietly(client);
                closeQuietly(upstream);
                counter.closed();
            }
        };
        executor.execute(() -> {
            pump(client, upstream, counter, true);
            directionDone.run();
        });
        pump(upstream, client, counter, false);
        directionDone.run();
    }

    /** Copies one direction; end of stream half-closes the other side, an error tears both down. */
    private static void pump(Socket from, Socket to, TrafficCounter counter, boolean inbound) {
        byte[] buffer = new byte[BUFFER_BYTES];
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                if (inbound) {
                    counter.addIn(read);
                } else {
                    counter.addOut(read);
                }
            }
            halfClose(to);
        } catch (IOException exception) {
            closeQuietly(from);
            closeQuietly(to);
        }
    }

    /** TLS sockets cannot half-close: the whole socket closes instead. */
    private static void halfClose(Socket socket) {
        try {
            socket.shutdownOutput();
        } catch (IOException | UnsupportedOperationException exception) {
            closeQuietly(socket);
        }
    }

    private static void answer(Socket client, byte[] response) {
        try {
            client.getOutputStream().write(response);
            halfClose(client);
        } catch (IOException ignored) {
            // the visitor already left
        } finally {
            closeQuietly(client);
        }
    }

    private static String clientAddress(Socket client) {
        return client.getRemoteSocketAddress() instanceof InetSocketAddress address
            ? address.getAddress().getHostAddress() : "desconhecido";
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException | RuntimeException ignored) {
            // nothing left to do
        }
    }
}
