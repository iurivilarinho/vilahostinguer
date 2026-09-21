package com.bancada.response;

import com.bancada.enums.ConnectionType;
import com.bancada.enums.DeviceStatus;
import com.bancada.enums.InitSystem;
import com.bancada.enums.PackageManager;
import com.bancada.models.Device;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Dispositivo com as informações do sistema")
public record DeviceResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome", example = "Galaxy J4+")
    String name,

    @Schema(description = "Endereço", example = "169.254.1.1")
    String host,

    @Schema(description = "Porta do SSH", example = "22")
    int port,

    @Schema(description = "Impressão digital da chave do servidor", example = "SHA256:0f3c...")
    String hostKeyFingerprint,

    @Schema(description = "Tipo da chave do servidor", example = "ssh-ed25519")
    String hostKeyType,

    @Schema(description = "Como está ligado")
    ConnectionType connectionType,

    @Schema(description = "Descrição da ligação", example = "Cabo USB")
    String connectionTypeDescription,

    @Schema(description = "Adaptador deste computador por onde foi visto", example = "Ethernet 4")
    String interfaceName,

    @Schema(description = "Situação de acesso")
    DeviceStatus status,

    @Schema(description = "Descrição da situação", example = "Pronto")
    String statusDescription,

    @Schema(description = "Respondeu na última verificação")
    boolean online,

    @Schema(description = "Última vez em que respondeu")
    LocalDateTime lastSeenAt,

    @Schema(description = "Credencial vinculada")
    CredentialBasicResponse credential,

    @Schema(description = "Nome do host", example = "samsung-j4primelte")
    String hostname,

    @Schema(description = "Sistema operacional", example = "postmarketOS")
    String osName,

    @Schema(description = "Versão do sistema", example = "edge")
    String osVersion,

    @Schema(description = "Kernel", example = "3.18.140")
    String kernelVersion,

    @Schema(description = "Arquitetura", example = "aarch64")
    String architecture,

    @Schema(description = "Processador", example = "Qualcomm MSM8917")
    String cpuModel,

    @Schema(description = "Núcleos", example = "4")
    Integer cpuCores,

    @Schema(description = "Memória total em bytes")
    Long memoryTotalBytes,

    @Schema(description = "Espaço total da raiz em bytes")
    Long diskTotalBytes,

    @Schema(description = "Modelo do aparelho", example = "Samsung Galaxy J4+")
    String model,

    @Schema(description = "Endereço MAC")
    String macAddress,

    @Schema(description = "Gerenciador de pacotes")
    PackageManager packageManager,

    @Schema(description = "Sistema de inicialização")
    InitSystem initSystem,

    @Schema(description = "Pasta pessoal do usuário da credencial", example = "/root")
    String homeDirectory,

    @Schema(description = "A credencial entra como root")
    boolean rootAccess,

    @Schema(description = "Quando as informações foram lidas")
    LocalDateTime factsUpdatedAt,

    @Schema(description = "Anotações")
    String notes,

    @Schema(description = "Ativo")
    boolean active,

    @Schema(description = "Data de cadastro")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public DeviceResponse(Device device) {
        this(device.getId(), device.getName(), device.getHost(), device.getPort(), device.getHostKeyFingerprint(),
            device.getHostKeyType(), device.getConnectionType(), device.getConnectionType().getDescription(),
            device.getInterfaceName(), device.getStatus(), device.getStatus().getDescription(), device.isOnline(),
            device.getLastSeenAt(), device.getCredential() == null ? null : new CredentialBasicResponse(device.getCredential()),
            device.getHostname(), device.getOsName(), device.getOsVersion(), device.getKernelVersion(),
            device.getArchitecture(), device.getCpuModel(), device.getCpuCores(), device.getMemoryTotalBytes(),
            device.getDiskTotalBytes(), device.getModel(), device.getMacAddress(), device.getPackageManager(),
            device.getInitSystem(), device.getHomeDirectory(), device.isRootAccess(), device.getFactsUpdatedAt(), device.getNotes(),
            device.isActive(), device.getCreatedAt(), device.getUpdatedAt());
    }
}
