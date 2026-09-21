package com.bancada.response;

import com.bancada.models.Domain;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de um domínio")
public record DomainBasicResponse(

    @Schema(description = "Identificador", example = "1")
    Long id,

    @Schema(description = "Nome completo", example = "casa.duckdns.org")
    String name
) {

    public DomainBasicResponse(Domain domain) {
        this(domain.getId(), domain.getName());
    }
}
