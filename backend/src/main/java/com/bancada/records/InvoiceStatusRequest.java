package com.bancada.records;

import com.bancada.enums.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Confirma à mão o pagamento de uma fatura, ou a cancela")
public record InvoiceStatusRequest(

    @Schema(description = "Nova situação (PAID ou CANCELED)", example = "PAID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Informe a situação")
    InvoiceStatus status,

    @Schema(description = "Observação (como foi pago, por que cancelou)", example = "Pix recebido no banco")
    @Size(max = 500, message = "Até 500 caracteres")
    String reason
) {
}
