package com.bancada.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Informações públicas do painel do cliente")
public record PortalInfoResponse(

    @Schema(description = "Nome da empresa", example = "VilaHost")
    String companyName,

    @Schema(description = "Cadastro de clientes novos aberto")
    boolean registrationOpen,

    @Schema(description = "Pagamento por Pix automático (Mercado Pago) disponível")
    boolean automaticPix,

    @Schema(description = "Instruções de pagamento manual")
    String manualPaymentInstructions,

    @Schema(description = "E-mail de suporte", example = "suporte@vilahost.com.br")
    String supportEmail
) {
}
