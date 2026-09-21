package com.bancada.config;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * The customer terminal accepts only pages of the panel itself: the Origin of the handshake must be
 * the host the request came to. Another site cannot open a shell with the visitor's cookie.
 */
public class SameOriginHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        String origin = request.getHeaders().getOrigin();
        String host = request.getHeaders().getFirst("X-Forwarded-Host");
        if (host == null) {
            host = request.getHeaders().getFirst(HttpHeaders.HOST);
        }
        if (origin == null || host == null) {
            return false;
        }
        try {
            URI originUri = URI.create(origin);
            String originHost = originUri.getPort() < 0 ? originUri.getHost() : originUri.getHost() + ":" + originUri.getPort();
            return originHost != null && (originHost.equalsIgnoreCase(host) || originUri.getHost().equalsIgnoreCase(host));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) {
        // nothing after the handshake
    }
}
