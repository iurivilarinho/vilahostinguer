package com.bancada.turnstile;

/** Outcome of checking a Turnstile token. */
public enum TurnstileVerdict {

	/** Cloudflare confirmed the token. */
	PASSED("", ""),

	/** The request carried no token. */
	MISSING("human-verification-required", "Conclua a verificação de segurança para continuar."),

	/** Cloudflare refused the token (expired, reused or forged). */
	REJECTED("human-verification-failed", "Não foi possível confirmar a verificação de segurança. Tente de novo."),

	/** Cloudflare could not be reached or answered with an error; the request fails closed. */
	UNAVAILABLE("human-verification-unavailable",
			"A verificação de segurança está indisponível agora. Tente de novo em instantes.");

	private final String code;
	private final String message;

	TurnstileVerdict(String code, String message) {
		this.code = code;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
