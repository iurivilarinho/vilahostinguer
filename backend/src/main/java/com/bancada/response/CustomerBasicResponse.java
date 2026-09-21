package com.bancada.response;

import com.bancada.models.Customer;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de um cliente")
public record CustomerBasicResponse(

    @Schema(description = "Identificador", example = "7")
    Long id,

    @Schema(description = "Nome", example = "Maria Souza")
    String name,

    @Schema(description = "E-mail", example = "maria@exemplo.com")
    String email
) {

    public CustomerBasicResponse(Customer customer) {
        this(customer.getId(), customer.getName(), customer.getEmail());
    }
}
