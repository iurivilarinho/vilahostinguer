package com.bancada.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Entrega um disco do PC a um dispositivo (montado numa pasta) ou a uma máquina")
public record VolumeAttachRequest(

    @Schema(description = "Dispositivo; com máquina informada, vale o dispositivo dela", example = "1")
    Long deviceId,

    @Schema(description = "Máquina que vai usar o disco (opcional)", example = "3")
    Long machineId,

    @Schema(description = "Pasta do dispositivo onde montar (sem ela: /mnt/<nome> ou, para máquina, /srv/bancada/discos/<nome>)",
        example = "/mnt/dados")
    @Pattern(regexp = "^$|^/[A-Za-z0-9._/-]+$", message = "Use um caminho absoluto sem espaços")
    String mountPath,

    @Schema(description = "Onde o disco aparece dentro da máquina (obrigatório com máquina)", example = "/dados")
    @Pattern(regexp = "^$|^/[A-Za-z0-9._/-]+$", message = "Use um caminho absoluto sem espaços")
    String containerPath
) {
}
