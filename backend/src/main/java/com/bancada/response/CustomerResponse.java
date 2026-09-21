package com.bancada.response;

import com.bancada.enums.CustomerStatus;
import com.bancada.models.Customer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Cliente")
public record CustomerResponse(

    @Schema(description = "Identificador", example = "7")
    Long id,

    @Schema(description = "Nome", example = "Maria Souza")
    String name,

    @Schema(description = "E-mail", example = "maria@exemplo.com")
    String email,

    @Schema(description = "Telefone", example = "34999990000")
    String phone,

    @Schema(description = "CPF ou CNPJ", example = "12345678909")
    String document,

    @Schema(description = "Situação da conta")
    CustomerStatus status,

    @Schema(description = "Descrição da situação", example = "Ativo")
    String statusDescription,

    @Schema(description = "Último acesso")
    LocalDateTime lastLoginAt,

    @Schema(description = "Data do cadastro")
    LocalDateTime createdAt,

    @Schema(description = "Data da última alteração")
    LocalDateTime updatedAt
) {

    public CustomerResponse(Customer customer) {
        this(customer.getId(), customer.getName(), customer.getEmail(), customer.getPhone(), customer.getDocument(), customer.getStatus(),
            customer.getStatus().getDescription(), customer.getLastLoginAt(), customer.getCreatedAt(), customer.getUpdatedAt());
    }
}
