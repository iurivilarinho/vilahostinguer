package com.bancada.response;

import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineNetworkMode;
import com.bancada.enums.MachineStatus;
import com.bancada.models.Machine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Máquina Linux")
public record MachineResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Dispositivo onde roda")
    DeviceBasicResponse device,

    @Schema(description = "Nome", example = "web-teste")
    String name,

    @Schema(description = "Nome do contêiner", example = "bancada-web-teste")
    String containerName,

    @Schema(description = "Distribuição")
    MachineDistribution distribution,

    @Schema(description = "Nome da distribuição", example = "Ubuntu")
    String distributionName,

    @Schema(description = "Versão", example = "24.04")
    String version,

    @Schema(description = "Imagem Docker", example = "ubuntu:24.04")
    String image,

    @Schema(description = "Limite de CPUs", example = "1.5")
    Double cpuLimit,

    @Schema(description = "Limite de memória em MB", example = "512")
    Integer memoryLimitMb,

    @Schema(description = "Rede")
    MachineNetworkMode networkMode,

    @Schema(description = "Descrição da rede", example = "Rede do dispositivo")
    String networkModeDescription,

    @Schema(description = "Portas encaminhadas")
    List<MachinePortResponse> ports,

    @Schema(description = "Pastas compartilhadas")
    List<MachineVolumeResponse> volumes,

    @Schema(description = "Usuário", example = "iuri")
    String username,

    @Schema(description = "SSH instalado")
    boolean sshEnabled,

    @Schema(description = "Porta do SSH dentro da máquina", example = "2201")
    Integer sshPort,

    @Schema(description = "Liga junto com o dispositivo")
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
        this(machine.getId(), new DeviceBasicResponse(machine.getDevice()), machine.getName(), machine.getContainerName(),
            machine.getDistribution(), machine.getDistribution().getDisplayName(), machine.getVersion(), machine.getImage(),
            machine.getCpuLimit(), machine.getMemoryLimitMb(), machine.getNetworkMode(), machine.getNetworkMode().getDescription(),
            machine.getPorts().stream().map(MachinePortResponse::new).toList(),
            machine.getVolumes().stream().map(MachineVolumeResponse::new).toList(), machine.getUsername(),
            machine.isSshEnabled(), machine.getSshPort(), machine.isAutoStart(), machine.getStatus(), machine.getStatus().getDescription(),
            machine.getCreatedAt(), machine.getUpdatedAt());
    }
}
