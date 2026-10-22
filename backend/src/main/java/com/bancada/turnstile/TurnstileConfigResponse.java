package com.bancada.turnstile;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuração pública da verificação de segurança (Cloudflare Turnstile)")
public record TurnstileConfigResponse(
		@Schema(description = "Se a verificação está ligada neste sistema") boolean enabled,
		@Schema(description = "Chave pública do widget; nula quando desligada", nullable = true) String siteKey) {
}
