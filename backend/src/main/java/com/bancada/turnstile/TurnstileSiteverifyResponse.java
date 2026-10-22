package com.bancada.turnstile;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Body of the Cloudflare siteverify answer; only the fields the check uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TurnstileSiteverifyResponse(boolean success, @JsonProperty("error-codes") List<String> errorCodes) {
}
