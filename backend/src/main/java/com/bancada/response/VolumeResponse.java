package com.bancada.response;

import com.bancada.enums.VolumeStatus;
import com.bancada.models.Volume;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Disco virtual guardado num disco deste PC (o nome secreto do NBD nunca sai do painel)")
public record VolumeResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome", example = "dados-site")
    String name,

    @Schema(description = "Disco do PC", example = "E:\\")
    String drive,

    @Schema(description = "Arquivo neste PC", example = "E:\\BancadaDiscos\\dados-site.img")
    String filePath,

    @Schema(description = "Tamanho em bytes", example = "107374182400")
    long sizeBytes,

    @Schema(description = "Bytes do disco do PC já ocupados (estimativa por cima)", example = "5368709120")
    long writtenBytes,

    @Schema(description = "Situação")
    VolumeStatus status,

    @Schema(description = "Descrição da situação", example = "Conectado")
    String statusDescription,

    @Schema(description = "Detalhe da situação")
    String statusMessage,

    @Schema(description = "Dispositivo que recebe o disco")
    DeviceBasicResponse device,

    @Schema(description = "Pasta do dispositivo onde fica montado", example = "/mnt/dados-site")
    String mountPath,

    @Schema(description = "Máquina que usa o disco")
    MachineBasicResponse machine,

    @Schema(description = "Onde aparece dentro da máquina", example = "/dados")
    String containerPath,

    @Schema(description = "Já tem sistema de arquivos")
    boolean formatted,

    @Schema(description = "Última conexão do dispositivo")
    LocalDateTime connectedAt,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public VolumeResponse(Volume volume, long writtenBytes) {
        this(volume.getId(), volume.getName(), volume.getDrive(), volume.getFilePath(), volume.getSizeBytes(), writtenBytes,
            volume.getStatus(), volume.getStatus().getDescription(), volume.getStatusMessage(),
            volume.getDevice() == null ? null : new DeviceBasicResponse(volume.getDevice()), volume.getMountPath(),
            volume.getMachine() == null ? null : new MachineBasicResponse(volume.getMachine()), volume.getContainerPath(),
            volume.isFormatted(), volume.getConnectedAt(), volume.getCreatedAt(), volume.getUpdatedAt());
    }
}
