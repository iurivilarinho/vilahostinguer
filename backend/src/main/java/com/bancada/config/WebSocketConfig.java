package com.bancada.config;

import com.bancada.controller.PortalTerminalWebSocketHandler;
import com.bancada.controller.TerminalWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TerminalWebSocketHandler terminalWebSocketHandler;
    private final PortalTerminalWebSocketHandler portalTerminalWebSocketHandler;

    public WebSocketConfig(TerminalWebSocketHandler terminalWebSocketHandler, PortalTerminalWebSocketHandler portalTerminalWebSocketHandler) {
        this.terminalWebSocketHandler = terminalWebSocketHandler;
        this.portalTerminalWebSocketHandler = portalTerminalWebSocketHandler;
    }

    /**
     * The administrator page is served by this same backend (or proxied by Vite in development), so
     * only local origins are accepted. The customer terminal is checked against the host it was
     * reached through, whatever name the panel is published under.
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalWebSocketHandler, "/ws/terminal")
            .setAllowedOriginPatterns("http://127.0.0.1:*", "http://localhost:*", "http://tauri.localhost", "tauri://localhost");
        registry.addHandler(portalTerminalWebSocketHandler, "/ws/portal/terminal")
            .addInterceptors(new SameOriginHandshakeInterceptor())
            .setAllowedOriginPatterns("*");
    }
}
