package com.bancada.response;

import com.bancada.enums.RouteStatus;
import com.bancada.enums.RouteType;
import com.bancada.models.Route;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Rota de acesso remoto")
public record RouteResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Tipo")
    RouteType type,

    @Schema(description = "Descrição do tipo", example = "Site (HTTP)")
    String typeDescription,

    @Schema(description = "Nome acessado (sites)", example = "blog.casa.duckdns.org")
    String hostname,

    @Schema(description = "Porta aberta no PC (TCP)", example = "2201")
    Integer publicPort,

    @Schema(description = "Dispositivo de destino")
    DeviceBasicResponse device,

    @Schema(description = "Máquina de destino")
    MachineBasicResponse machine,

    @Schema(description = "Domínio cadastrado que cobre o nome")
    DomainBasicResponse domain,

    @Schema(description = "Porta do serviço no destino", example = "80")
    int targetPort,

    @Schema(description = "Observação", example = "Blog da família")
    String description,

    @Schema(description = "Situação")
    RouteStatus status,

    @Schema(description = "Descrição da situação", example = "Ativa")
    String statusDescription,

    @Schema(description = "Data de criação")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public RouteResponse(Route route) {
        this(route.getId(), route.getType(), route.getType().getDescription(), route.getHostname(), route.getPublicPort(),
            new DeviceBasicResponse(route.getDevice()),
            route.getMachine() == null ? null : new MachineBasicResponse(route.getMachine()),
            route.getDomain() == null ? null : new DomainBasicResponse(route.getDomain()),
            route.getTargetPort(), route.getDescription(), route.getStatus(), route.getStatus().getDescription(),
            route.getCreatedAt(), route.getUpdatedAt());
    }
}
