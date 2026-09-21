package com.bancada.gateway;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bancada.enums.RouteType;
import com.bancada.records.RouteTarget;
import com.bancada.service.PortalCertificateService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The gateway ends TLS for the customer panel and passes plain HTTP marked as https, one request per connection. */
class TlsTerminationTest {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private static X509Certificate selfSigned(KeyPair keyPair, String name) throws Exception {
        X500Name subject = new X500Name("CN=" + name);
        Date now = new Date();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(subject, BigInteger.valueOf(now.getTime()), now,
            new Date(now.getTime() + 86_400_000L), subject, keyPair.getPublic());
        return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate())));
    }

    @Test
    void panelRequestArrivesDecryptedWithForwardedProtoAndConnectionClose() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        SSLContext serverContext = PortalCertificateService.buildContext(keyPair, List.of(selfSigned(keyPair, "painel.exemplo.com")));

        ServerSocket upstream = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        StringBuilder received = new StringBuilder();
        executor.execute(() -> {
            try (Socket socket = upstream.accept()) {
                received.append(new String(GatewayListenerTest.HttpRequestHeadTestSupport.readHead(socket.getInputStream()), StandardCharsets.ISO_8859_1));
                socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".getBytes(StandardCharsets.ISO_8859_1));
            } catch (IOException ignored) {
                // test ends
            }
        });
        RouteTarget panel = new RouteTarget(0L, RouteType.TLS, "painel.exemplo.com", null, "127.0.0.1", upstream.getLocalPort(), "Painel",
            serverContext, true);
        int port;
        try (ServerSocket probe = new ServerSocket(0)) {
            port = probe.getLocalPort();
        }
        GatewayListener listener = GatewayListener.open(port, RouteType.TLS, executor, Map.of("painel.exemplo.com", panel)::get,
            id -> new TrafficCounter());

        SSLContext trustAll = SSLContext.getInstance("TLS");
        trustAll.init(null, new TrustManager[] {new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new SecureRandom());
        String answer;
        try (SSLSocket client = (SSLSocket) trustAll.getSocketFactory().createSocket(InetAddress.getLoopbackAddress(), port)) {
            SSLParameters parameters = client.getSSLParameters();
            parameters.setServerNames(List.of(new SNIHostName("painel.exemplo.com")));
            client.setSSLParameters(parameters);
            client.setSoTimeout(5_000);
            client.getOutputStream().write("GET /painel HTTP/1.1\r\nHost: painel.exemplo.com\r\nConnection: keep-alive\r\n\r\n"
                .getBytes(StandardCharsets.ISO_8859_1));
            InputStream in = client.getInputStream();
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            in.transferTo(body);
            answer = body.toString(StandardCharsets.ISO_8859_1);
        } finally {
            listener.close();
            upstream.close();
        }

        assertTrue(answer.endsWith("ok"), answer);
        assertTrue(received.toString().contains("X-Forwarded-Proto: https"), received.toString());
        assertTrue(received.toString().contains("Connection: close"));
        assertTrue(!received.toString().contains("keep-alive"), "the visitor's keep-alive is replaced");
    }
}
