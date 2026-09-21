package com.bancada.controller;

import com.bancada.models.Device;
import com.bancada.models.Machine;
import com.bancada.records.ShellSession;
import com.bancada.service.DeviceService;
import com.bancada.service.MachineService;
import com.bancada.service.SshService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.JSchException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/** Administrator terminal: a root shell on a device ({@code deviceId}) or a shell inside a machine ({@code machineId}). */
@Component
public class TerminalWebSocketHandler extends ShellWebSocketHandler {

    private final DeviceService deviceService;
    private final SshService sshService;
    private final MachineService machineService;

    public TerminalWebSocketHandler(DeviceService deviceService, SshService sshService, MachineService machineService,
                                    ObjectMapper objectMapper) {
        super(objectMapper);
        this.deviceService = deviceService;
        this.sshService = sshService;
        this.machineService = machineService;
    }

    @Override
    protected ShellSession open(WebSocketSession session, Map<String, String> query, int columns, int rows) throws JSchException {
        if (query.containsKey("machineId")) {
            // Terminal inside a machine: docker exec with a PTY on the device that hosts it.
            Machine machine = machineService.findById(Long.parseLong(query.get("machineId")));
            Device device = deviceService.requireReady(machine.getDevice().getId());
            return new ShellSession(sshService.openCommandPty(device, machineService.shellCommand(machine), true, columns, rows),
                "terminal-machine-" + machine.getId());
        }
        Device device = deviceService.requireReady(Long.parseLong(query.getOrDefault("deviceId", "")));
        return new ShellSession(sshService.openShell(device, columns, rows), "terminal-" + device.getId());
    }
}
