package com.bancada.response;

import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineStatus;
import com.bancada.models.Machine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Máquina virtual Linux deste PC")
public record MachineResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "O dispositivo que representa a máquina no painel (terminal, aplicativos, arquivos)")
    DeviceBasicResponse device,

    @Schema(description = "Nome", example = "web-1")
    String name,

    @Schema(description = "Nome da VM no Hyper-V", example = "bancada-web-1")
    String vmName,

    @Schema(description = "Distribuição")
    MachineDistribution distribution,

    @Schema(description = "Nome da distribuição", example = "Ubuntu")
    String distributionName,

    @Schema(description = "Versão", example = "24.04")
    String version,

    @Schema(description = "Processadores virtuais", example = "2")
    int cpuCount,

    @Schema(description = "Memória em MB", example = "2048")
    int memoryMb,

    @Schema(description = "Disco em GB", example = "20")
    int diskGb,

    @Schema(description = "Disco do PC onde fica", example = "E:\\")
    String drive,

    @Schema(description = "Endereço fixo na rede das máquinas", example = "10.77.0.10")
    String ipAddress,

    @Schema(description = "Usuário com sudo", example = "admin")
    String username,

    @Schema(description = "Liga junto com o PC")
    boolean autoStart,

    @Schema(description = "Situação")
    MachineStatus status,

    @Schema(description = "Descrição da situação", example = "Ligada")
    String statusDescription,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public MachineResponse(Machine machine) {
        this(machine.getId(), machine.getDevice() == null ? null : new DeviceBasicResponse(machine.getDevice()), machine.getName(),
            machine.getVmName(), machine.getDistribution(), machine.getDistribution().getDisplayName(), machine.getVersion(),
            machine.getCpuCount(), machine.getMemoryMb(), machine.getDiskGb(), machine.getDrive(), machine.getIpAddress(),
            machine.getUsername(), machine.isAutoStart(), machine.getStatus(), machine.getStatus().getDescription(),
            machine.getCreatedAt(), machine.getUpdatedAt());
    }
}
