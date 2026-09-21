package com.bancada.request;

import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineNetworkMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Criação de máquina Linux (contêiner Docker de sistema)")
public record MachineRequest(

    @Schema(description = "Dispositivo onde a máquina roda", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe o dispositivo")
    Long deviceId,

    @Schema(description = "Nome da máquina", example = "web-teste", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o nome")
    @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,40}", message = "Use letras minúsculas, números e hífen (2 a 41 caracteres)")
    String name,

    @Schema(description = "Distribuição", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha a distribuição")
    MachineDistribution distribution,

    @Schema(description = "Versão da distribuição", example = "24.04", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Escolha a versão")
    String version,

    @Schema(description = "Limite de CPUs (vazio = sem limite)", example = "1.5")
    @DecimalMin(value = "0.1", message = "Mínimo de 0,1 CPU")
    @DecimalMax(value = "64", message = "Máximo de 64 CPUs")
    Double cpuLimit,

    @Schema(description = "Limite de memória em MB (vazio = sem limite)", example = "512")
    @Min(value = 32, message = "Mínimo de 32 MB")
    @Max(value = 262_144, message = "Máximo de 256 GB")
    Integer memoryLimitMb,

    @Schema(description = "Rede", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Escolha a rede")
    MachineNetworkMode networkMode,

    @Schema(description = "Portas encaminhadas (só na rede isolada)")
    @NotNull
    List<@Valid MachinePortRequest> ports,

    @Schema(description = "Pastas compartilhadas")
    @NotNull
    List<@Valid MachineVolumeRequest> volumes,

    @Schema(description = "Usuário criado dentro da máquina", example = "iuri", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe o usuário")
    @Pattern(regexp = "[a-z_][a-z0-9_-]{0,31}", message = "Usuário Linux inválido")
    String username,

    @Schema(description = "Senha do usuário (e do sudo)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Informe a senha")
    @Size(min = 4, max = 128, message = "Use de 4 a 128 caracteres")
    String password,

    @Schema(description = "Instalar e ligar o servidor SSH", example = "true")
    boolean installSsh,

    @Schema(description = "Porta do SSH dentro da máquina (na rede do dispositivo, não pode ser a 22)", example = "2201")
    @Min(value = 1, message = "Porta inválida")
    @Max(value = 65535, message = "Porta inválida")
    Integer sshPort,

    @Schema(description = "Ligar junto com o dispositivo", example = "true")
    boolean autoStart
) {
}
