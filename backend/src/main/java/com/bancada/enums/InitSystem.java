package com.bancada.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sistema de inicialização (gerência de serviços) do dispositivo")
public enum InitSystem {

    @Schema(description = "systemd")
    SYSTEMD("systemd"),

    @Schema(description = "OpenRC — Alpine e postmarketOS")
    OPENRC("openrc");

    private final String label;

    InitSystem(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String serviceCommand(String service, ServiceAction action) {
        if (this == SYSTEMD) {
            return "systemctl " + action.name().toLowerCase() + " " + service;
        }
        return switch (action) {
            case ENABLE -> "rc-update add " + service + " default";
            case DISABLE -> "rc-update del " + service + " default";
            default -> "rc-service " + service + " " + action.name().toLowerCase();
        };
    }

    /** Shell snippet that prints {@code running=0|1} and {@code enabled=0|1} for the service. */
    public String statusSnippet(String service) {
        if (this == SYSTEMD) {
            return "systemctl is-active --quiet " + service + " && echo running=1 || echo running=0; "
                + "systemctl is-enabled --quiet " + service + " 2>/dev/null && echo enabled=1 || echo enabled=0";
        }
        return "rc-service " + service + " status >/dev/null 2>&1 && echo running=1 || echo running=0; "
            + "rc-update show default 2>/dev/null | grep -qw " + service + " && echo enabled=1 || echo enabled=0";
    }

    public static InitSystem fromLabel(String label) {
        for (InitSystem system : values()) {
            if (system.label.equals(label)) {
                return system;
            }
        }
        return null;
    }
}
