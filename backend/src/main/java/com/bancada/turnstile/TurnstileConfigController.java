package com.bancada.turnstile;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Tells the front whether to draw the widget, and with which public key. Public route. */
@RestController
@Tag(name = "Verificação de segurança", description = "Cloudflare Turnstile nas telas de acesso")
public class TurnstileConfigController {

	private final TurnstileProperties properties;

	public TurnstileConfigController(TurnstileProperties properties) {
		this.properties = properties;
	}

	@Operation(summary = "Configuração da verificação de segurança",
			description = "Responde se o Turnstile está ligado e a chave pública do widget. Nunca devolve a chave secreta.")
	@ApiResponse(responseCode = "200", description = "Configuração atual")
	@GetMapping("${turnstile.config-path:/public/turnstile}")
	public ResponseEntity<TurnstileConfigResponse> config() {
		TurnstileConfigResponse body = new TurnstileConfigResponse(properties.enabled(),
				properties.enabled() ? properties.siteKey() : null);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
	}
}
