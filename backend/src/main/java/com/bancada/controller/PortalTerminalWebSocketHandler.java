package com.bancada.controller;

import com.bancada.exception.PortalAuthenticationException;
import com.bancada.models.Device;
import com.bancada.models.Machine;
import com.bancada.records.ShellSession;
import com.bancada.service.DeviceService;
import com.bancada.service.PortalServerService;
import com.bancada.service.SshService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * Customer terminal: a shell inside one of their own servers ({@code serverId} = subscription). The
 * customer comes from the authenticated handshake; a server of someone else is refused.
 */
@Component
public class PortalTerminalWebSocketHandler extends ShellWebSocketHandler {

    private final PortalServerService portalServerService;
    private final DeviceService deviceService;
    private final SshService sshService;

    public PortalTerminalWebSocketHandler(PortalServerService portalServerService, DeviceService deviceService,
                                          SshService sshService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.portalServerService = portalServerService;
        this.deviceService = deviceService;
        this.sshService = sshService;
    }

    @Override
    protected ShellSession open(WebSocketSession session, Map<String, String> query, int columns, int rows) throws JSchException {
        Principal principal = session.getPrincipal();
        if (!(principal instanceof Authentication authentication) || !(authentication.getPrincipal() instanceof Long customerId)) {
            throw new PortalAuthenticationException("Sua sessão expirou. Entre de novo.");
        }
        Machine machine = portalServerService.terminalMachine(customerId, Long.parseLong(query.getOrDefault("serverId", "")));
        Device device = deviceService.requireReady(machine.getDevice().getId());
        return new ShellSession(sshService.openShell(device, columns, rows), "portal-terminal-" + machine.getId());
    }
}
