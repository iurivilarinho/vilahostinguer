package com.bancada.gateway;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Head of the first HTTP request on a connection: enough to pick the route by the Host header and
 * to pass the request on with the visitor address in the X-Forwarded-* headers. Everything after
 * the head (body, further requests) is relayed untouched.
 */
public final class HttpRequestHead {

    static final int MAX_HEAD_BYTES = 32 * 1024;
    private static final byte[] HEAD_END = {'\r', '\n', '\r', '\n'};
    private static final List<String> FORWARDING_HEADERS = List.of("x-forwarded-for", "x-real-ip", "x-forwarded-proto", "x-forwarded-host");
    private static final List<String> CONNECTION_HEADERS = List.of("connection", "keep-alive", "proxy-connection");

    private final String[] lines;
    private final byte[] rest;
    private final String rawHost;
    private final String host;

    private HttpRequestHead(String[] lines, byte[] rest, String rawHost) {
        this.lines = lines;
        this.rest = rest;
        this.rawHost = rawHost;
        this.host = normalizeHost(rawHost);
    }

    /** Reads up to the blank line that ends the head; null when the connection closes first. */
    public static HttpRequestHead read(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
        byte[] chunk = new byte[4096];
        int end = -1;
        while (end < 0) {
            int read = in.read(chunk);
            if (read < 0) {
                return null;
            }
            int searchFrom = Math.max(0, buffer.size() - HEAD_END.length + 1);
            buffer.write(chunk, 0, read);
            if (buffer.size() > MAX_HEAD_BYTES) {
                throw new IOException("Cabeçalho HTTP maior que " + MAX_HEAD_BYTES + " bytes");
            }
            end = indexOf(buffer.toByteArray(), searchFrom);
        }
        byte[] all = buffer.toByteArray();
        String head = new String(all, 0, end, StandardCharsets.ISO_8859_1);
        String[] lines = head.split("\r\n", -1);
        String rawHost = null;
        for (int index = 1; index < lines.length; index++) {
            int colon = lines[index].indexOf(':');
            if (colon > 0 && lines[index].substring(0, colon).trim().equalsIgnoreCase("host")) {
                rawHost = lines[index].substring(colon + 1).trim();
                break;
            }
        }
        return new HttpRequestHead(lines, Arrays.copyOfRange(all, end + HEAD_END.length, all.length), rawHost);
    }

    /** The head as it goes to the destination: forwarding headers from the visitor dropped, ours added. */
    public byte[] forwardedBytes(String clientAddress) {
        return forwardedBytes(clientAddress, "http", false);
    }

    /**
     * @param protocol what the visitor used ({@code http}, or {@code https} when the gateway ended TLS)
     * @param closeAfter one request per connection: {@code Connection: close} replaces the visitor's
     *                   keep-alive, so the next request comes on a new connection with fresh headers
     */
    public byte[] forwardedBytes(String clientAddress, String protocol, boolean closeAfter) {
        StringBuilder head = new StringBuilder(lines[0]).append("\r\n");
        for (int index = 1; index < lines.length; index++) {
            int colon = lines[index].indexOf(':');
            String name = colon > 0 ? lines[index].substring(0, colon).trim().toLowerCase(Locale.ROOT) : "";
            if (!FORWARDING_HEADERS.contains(name) && !(closeAfter && CONNECTION_HEADERS.contains(name))) {
                head.append(lines[index]).append("\r\n");
            }
        }
        head.append("X-Forwarded-For: ").append(clientAddress).append("\r\n")
            .append("X-Real-IP: ").append(clientAddress).append("\r\n")
            .append("X-Forwarded-Proto: ").append(protocol).append("\r\n");
        if (rawHost != null) {
            head.append("X-Forwarded-Host: ").append(rawHost).append("\r\n");
        }
        if (closeAfter) {
            head.append("Connection: close\r\n");
        }
        head.append("\r\n");
        byte[] headBytes = head.toString().getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = Arrays.copyOf(headBytes, headBytes.length + rest.length);
        System.arraycopy(rest, 0, result, headBytes.length, rest.length);
        return result;
    }

    /** Lowercase host without port and trailing dot; null when the request has no Host header. */
    public String host() {
        return host;
    }

    /** A complete small HTML response, closing the connection. */
    public static byte[] page(int status, String reason, String title, String detail) {
        String body = "<!doctype html><html lang=\"pt-BR\"><meta charset=\"utf-8\"><title>" + escape(title) + "</title>"
            + "<body style=\"font-family:system-ui,sans-serif;background:#f4f5ff;color:#2f1c6a;display:grid;place-items:center;"
            + "min-height:100vh;margin:0\"><main style=\"max-width:32rem;padding:2rem;background:#fff;border-radius:12px;"
            + "box-shadow:0 4px 24px #673de61a\"><h1 style=\"font-size:1.25rem\">" + escape(title) + "</h1><p>" + escape(detail)
            + "</p><p style=\"color:#727586;font-size:.875rem\">Bancada</p></main></body></html>";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + " " + reason + "\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "
            + bodyBytes.length + "\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n";
        byte[] headBytes = head.getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = Arrays.copyOf(headBytes, headBytes.length + bodyBytes.length);
        System.arraycopy(bodyBytes, 0, result, headBytes.length, bodyBytes.length);
        return result;
    }

    static String normalizeHost(String rawHost) {
        if (rawHost == null || rawHost.isBlank()) {
            return null;
        }
        String host = rawHost.trim().toLowerCase(Locale.ROOT);
        if (host.startsWith("[")) {
            int close = host.indexOf(']');
            host = close > 0 ? host.substring(1, close) : host;
        } else {
            int colon = host.lastIndexOf(':');
            if (colon > 0) {
                host = host.substring(0, colon);
            }
        }
        return host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
    }

    private static int indexOf(byte[] data, int from) {
        for (int index = from; index <= data.length - HEAD_END.length; index++) {
            if (data[index] == '\r' && data[index + 1] == '\n' && data[index + 2] == '\r' && data[index + 3] == '\n') {
                return index;
            }
        }
        return -1;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
