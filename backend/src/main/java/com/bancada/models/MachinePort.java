package com.bancada.models;

import com.bancada.request.MachinePortRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
@Schema(description = "Porta do dispositivo encaminhada para a máquina")
public class MachinePort {

    @Column(name = "host_port", nullable = false)
    @Schema(description = "Porta no dispositivo")
    private int hostPort;

    @Column(name = "container_port", nullable = false)
    @Schema(description = "Porta dentro da máquina")
    private int containerPort;

    @Column(name = "protocol", nullable = false)
    @Schema(description = "Protocolo (tcp ou udp)")
    private String protocol;

    public MachinePort() {
    }

    public MachinePort(MachinePortRequest request) {
        this.hostPort = request.hostPort();
        this.containerPort = request.containerPort();
        this.protocol = request.protocol() == null || request.protocol().isBlank() ? "tcp" : request.protocol().toLowerCase();
    }

    public int getHostPort() {
        return hostPort;
    }

    public int getContainerPort() {
        return containerPort;
    }

    public String getProtocol() {
        return protocol;
    }
}
