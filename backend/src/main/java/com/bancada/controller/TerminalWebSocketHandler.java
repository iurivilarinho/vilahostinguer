package com.bancada.controller;

import com.bancada.models.Device;
import com.bancada.models.Machine;
import com.bancada.service.MachineService;
import com.bancada.service.DeviceService;
import com.bancada.service.SshService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Bridges the xterm.js terminal of the window to an SSH shell with a PTY.
 *
 * <p>Protocol: the browser sends JSON text frames, {@code {"type":"input","data":"..."}} for keys
 * and {@code {"type":"resize","cols":120,"rows":32}} for size changes. The device output goes back
 * as raw binary frames, left for xterm.js to decode, so a multi-byte character split between two
 * reads is never mangled here.
 */
@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TerminalWebSocketHandler.class);
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int SEND_BUFFER_LIMIT = 1024 * 1024;
    private static final int READ_BUFFER_SIZE = 16 * 1024;
    private static final int DEFAULT_COLUMNS = 120;
    private static final int DEFAULT_ROWS = 32;

    private final DeviceService deviceService;
    private final SshService sshService;
    private final MachineService machineService;
    private final ObjectMapper objectMapper;
    private final Map<String, Channel> shells = new ConcurrentHashMap<>();

    public TerminalWebSocketHandler(DeviceService deviceService, SshService sshService, MachineService machineService,
                                    ObjectMapper objectMapper) {
        this.deviceService = deviceService;
        this.sshService = sshService;
        this.machineService = machineService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession rawSession) throws IOException {
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(rawSession, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT);
        URI uri = rawSession.getUri();
        Map<String, String> query = uri == null ? Map.of()
            : UriComponentsBuilder.fromUri(uri).build().getQueryParams().toSingleValueMap();
        try {
            int columns = parseOrDefault(query.get("cols"), DEFAULT_COLUMNS);
            int rows = parseOrDefault(query.get("rows"), DEFAULT_ROWS);
            Channel shell;
            String threadName;
            if (query.containsKey("machineId")) {
                // Terminal inside a machine: docker exec with a PTY on the device that hosts it.
                Machine machine = machineService.findById(Long.parseLong(query.get("machineId")));
                Device device = deviceService.requireReady(machine.getDevice().getId());
                shell = sshService.openCommandPty(device, machineService.shellCommand(machine), true, columns, rows);
                threadName = "terminal-machine-" + machine.getId();
            } else {
                Device device = deviceService.requireReady(Long.parseLong(query.getOrDefault("deviceId", "")));
                shell = sshService.openShell(device, columns, rows);
                threadName = "terminal-" + device.getId();
            }
            InputStream output = shell.getInputStream();
            shell.connect(10_000);
            shells.put(rawSession.getId(), shell);
            Channel connected = shell;
            Thread reader = new Thread(() -> pump(output, session, connected), threadName);
            reader.setDaemon(true);
            reader.start();
        } catch (NumberFormatException exception) {
            closeWithMessage(session, "Dispositivo ou máquina não informado.");
        } catch (RuntimeException | JSchException exception) {
            closeWithMessage(session, exception.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Channel shell = shells.get(session.getId());
        if (shell == null) {
            return;
        }
        JsonNode payload = objectMapper.readTree(message.getPayload());
        String type = payload.path("type").asText();
        if ("input".equals(type)) {
            OutputStream input = shell.getOutputStream();
            input.write(payload.path("data").asText().getBytes(StandardCharsets.UTF_8));
            input.flush();
        } else if ("resize".equals(type)) {
            int columns = payload.path("cols").asInt(DEFAULT_COLUMNS);
            int rows = payload.path("rows").asInt(DEFAULT_ROWS);
            if (shell instanceof ChannelShell interactive) {
                interactive.setPtySize(columns, rows, 0, 0);
            } else if (shell instanceof ChannelExec command) {
                command.setPtySize(columns, rows, 0, 0);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Channel shell = shells.remove(session.getId());
        if (shell != null) {
            shell.disconnect();
        }
    }

    private void pump(InputStream output, WebSocketSession session, Channel shell) {
        byte[] buffer = new byte[READ_BUFFER_SIZE];
        try {
            int read;
            while ((read = output.read(buffer)) != -1 && session.isOpen()) {
                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);
                session.sendMessage(new BinaryMessage(chunk));
            }
        } catch (IOException exception) {
            LOG.debug("Terminal stream closed: {}", exception.getMessage());
        } finally {
            shell.disconnect();
            if (session.isOpen()) {
                try {
                    session.close(CloseStatus.NORMAL.withReason("Sessão encerrada no dispositivo"));
                } catch (IOException exception) {
                    LOG.debug("Could not close terminal socket: {}", exception.getMessage());
                }
            }
        }
    }

    private void closeWithMessage(WebSocketSession session, String message) throws IOException {
        String text = "\r\n\u001b[31m" + (message == null ? "Não foi possível abrir o terminal." : message) + "\u001b[0m\r\n";
        session.sendMessage(new BinaryMessage(text.getBytes(StandardCharsets.UTF_8)));
        session.close(CloseStatus.POLICY_VIOLATION.withReason("terminal"));
    }

    private static int parseOrDefault(String value, int fallback) {
        try {
            return value == null ? fallback : Math.max(10, Math.min(1000, Integer.parseInt(value)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
