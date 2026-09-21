package com.bancada.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bancada.enums.RouteType;
import com.bancada.records.RouteTarget;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GatewayListenerTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<Long, TrafficCounter> traffic = new ConcurrentHashMap<>();
    private final List<AutoCloseable> closeables = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }
        executor.shutdownNow();
    }

    /** Upstream that answers every connection with what it received, prefixed by its name. */
    private int upstream(String name) throws IOException {
        ServerSocket server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        closeables.add(server);
        executor.execute(() -> {
            while (!server.isClosed()) {
                try {
                    Socket socket = server.accept();
                    executor.execute(() -> {
                        try (socket) {
                            byte[] received = HttpRequestHeadTestSupport.readHead(socket.getInputStream());
                            String body = name + "\n" + new String(received, StandardCharsets.ISO_8859_1);
                            byte[] bytes = body.getBytes(StandardCharsets.ISO_8859_1);
                            socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: " + bytes.length
                                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
                            socket.getOutputStream().write(bytes);
                        } catch (IOException ignored) {
                            // test client left
                        }
                    });
                } catch (IOException closed) {
                    return;
                }
            }
        });
        return server.getLocalPort();
    }

    private int freePort() throws IOException {
        try (ServerSocket probe = new ServerSocket(0)) {
            return probe.getLocalPort();
        }
    }

    private GatewayListener open(RouteType type, Function<String, RouteTarget> resolver) throws IOException {
        GatewayListener listener = GatewayListener.open(freePort(), type, executor, resolver,
            id -> traffic.computeIfAbsent(id, key -> new TrafficCounter()));
        closeables.add(listener);
        return listener;
    }

    private static String exchange(int port, String request) throws IOException {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            socket.setSoTimeout(5_000);
            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes(StandardCharsets.ISO_8859_1));
            socket.shutdownOutput();
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void httpIsRoutedByHostWithForwardingHeadersAndSpoofedOnesDropped() throws IOException {
        int blog = upstream("blog");
        int shop = upstream("loja");
        Map<String, RouteTarget> routes = Map.of(
            "blog.casa.duckdns.org", new RouteTarget(1L, RouteType.HTTP, "blog.casa.duckdns.org", null, "127.0.0.1", blog, "A máquina blog"),
            "loja.casa.duckdns.org", new RouteTarget(2L, RouteType.HTTP, "loja.casa.duckdns.org", null, "127.0.0.1", shop, "A máquina loja"));
        GatewayListener listener = open(RouteType.HTTP, routes::get);

        String blogAnswer = exchange(listener.getPort(),
            "GET / HTTP/1.1\r\nHost: Blog.Casa.DuckDNS.org:80\r\nX-Forwarded-For: 6.6.6.6\r\n\r\n");
        String shopAnswer = exchange(listener.getPort(), "GET /carrinho HTTP/1.1\r\nHost: loja.casa.duckdns.org\r\n\r\n");

        assertTrue(blogAnswer.contains("\r\n\r\nblog\n"), blogAnswer);
        assertTrue(blogAnswer.contains("X-Forwarded-For: 127.0.0.1"));
        assertTrue(blogAnswer.contains("X-Forwarded-Host: Blog.Casa.DuckDNS.org:80"));
        assertFalse(blogAnswer.contains("6.6.6.6"), "the visitor cannot choose its own forwarded address");
        assertTrue(shopAnswer.contains("loja\nGET /carrinho HTTP/1.1"));
        assertEquals(1, traffic.get(1L).toResponse(1L).totalConnections());
        assertTrue(traffic.get(2L).toResponse(2L).bytesOut() > 0);
    }

    @Test
    void unknownHostGetsA404PageAndDeadTargetA502() throws IOException {
        int dead = freePort();
        GatewayListener listener = open(RouteType.HTTP,
            Map.of("parado.exemplo.com", new RouteTarget(3L, RouteType.HTTP, "parado.exemplo.com", null, "127.0.0.1", dead, "A máquina parada"))::get);

        String unknown = exchange(listener.getPort(), "GET / HTTP/1.1\r\nHost: outro.exemplo.com\r\n\r\n");
        String down = exchange(listener.getPort(), "GET / HTTP/1.1\r\nHost: parado.exemplo.com\r\n\r\n");

        assertTrue(unknown.startsWith("HTTP/1.1 404"));
        assertTrue(unknown.contains("outro.exemplo.com"));
        assertTrue(down.startsWith("HTTP/1.1 502"));
        assertTrue(traffic.get(3L).toResponse(3L).lastError().contains("não respondeu"));
    }

    @Test
    void tcpRouteRelaysBothWays() throws IOException {
        int ssh = upstream("ssh");
        RouteTarget target = new RouteTarget(4L, RouteType.TCP, null, 2201, "127.0.0.1", ssh, "A máquina ssh");
        GatewayListener listener = open(RouteType.TCP, name -> target);

        String answer = exchange(listener.getPort(), "SSH-2.0-teste\r\n\r\n");

        assertTrue(answer.contains("ssh\nSSH-2.0-teste"));
        assertEquals(0, traffic.get(4L).toResponse(4L).activeConnections());
    }

    @Test
    void serverNameIsReadFromARealClientHello() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        SSLEngine engine = context.createSSLEngine("site.casa.duckdns.org", 443);
        engine.setUseClientMode(true);
        SSLParameters parameters = engine.getSSLParameters();
        parameters.setServerNames(List.of(new SNIHostName("site.casa.duckdns.org")));
        engine.setSSLParameters(parameters);
        ByteBuffer out = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
        engine.wrap(ByteBuffer.allocate(0), out);
        out.flip();
        byte[] record = new byte[out.remaining()];
        out.get(record);

        TlsClientHello hello = TlsClientHello.read(new ByteArrayInputStream(record));

        assertEquals("site.casa.duckdns.org", hello.serverName());
        assertEquals(record.length, hello.bytes().length);
        assertNull(TlsClientHello.read(new ByteArrayInputStream("GET / HTTP/1.1\r\n".getBytes(StandardCharsets.US_ASCII))).serverName());
    }

    @Test
    void hostHeaderIsNormalized() {
        assertEquals("blog.exemplo.com", HttpRequestHead.normalizeHost("BLOG.exemplo.com.:8080"));
        assertEquals("::1", HttpRequestHead.normalizeHost("[::1]:80"));
        assertNull(HttpRequestHead.normalizeHost(" "));
    }

    /** Reads until the blank line that ends an HTTP head (or the end of the stream). */
    static final class HttpRequestHeadTestSupport {

        private HttpRequestHeadTestSupport() {
        }

        static byte[] readHead(InputStream in) throws IOException {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            int previous3 = 0;
            int previous2 = 0;
            int previous1 = 0;
            int current;
            while ((current = in.read()) >= 0) {
                buffer.write(current);
                if (previous3 == '\r' && previous2 == '\n' && previous1 == '\r' && current == '\n') {
                    break;
                }
                previous3 = previous2;
                previous2 = previous1;
                previous1 = current;
            }
            return buffer.toByteArray();
        }
    }
}
