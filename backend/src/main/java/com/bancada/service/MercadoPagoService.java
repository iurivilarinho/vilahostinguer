package com.bancada.service;

import com.bancada.models.Invoice;
import com.bancada.records.PixCharge;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Pix through the Mercado Pago payments API: one payment per invoice attempt, confirmed by polling
 * (no webhook, so the panel does not need to receive calls from Mercado Pago).
 */
@Service
public class MercadoPagoService {

    private static final String API = "https://api.mercadopago.com/v1/payments";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration PIX_VALIDITY = Duration.ofHours(24);
    private static final DateTimeFormatter EXPIRATION = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MercadoPagoService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public PixCharge createPix(Invoice invoice, String accessToken) {
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(PIX_VALIDITY);
        String firstName = invoice.getCustomer().getName().split("\\s+")[0];
        ObjectNode payer = objectMapper.createObjectNode().put("email", invoice.getCustomer().getEmail()).put("first_name", firstName);
        ObjectNode body = objectMapper.createObjectNode()
            .put("transaction_amount", invoice.getAmount())
            .put("description", invoice.getDescription())
            .put("payment_method_id", "pix")
            .put("external_reference", "fatura-" + invoice.getId())
            .put("date_of_expiration", EXPIRATION.format(expiresAt));
        body.set("payer", payer);
        HttpRequest request = HttpRequest.newBuilder(URI.create(API))
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .header("X-Idempotency-Key", "fatura-" + invoice.getId() + "-" + UUID.randomUUID())
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        JsonNode payment = send(request);
        JsonNode data = payment.path("point_of_interaction").path("transaction_data");
        String code = data.path("qr_code").asText(null);
        if (code == null) {
            throw new IllegalStateException("O Mercado Pago não devolveu o código Pix. Confira se a conta aceita Pix.");
        }
        return new PixCharge(payment.path("id").asText(), code, data.path("qr_code_base64").asText(null),
            expiresAt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
    }

    /** Payment status at Mercado Pago: pending, approved, rejected, cancelled, expired... */
    public String status(String paymentId, String accessToken) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + "/" + paymentId))
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
        return send(request).path("status").asText("unknown");
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new IllegalStateException("O Mercado Pago recusou o access token. Confira nas preferências do painel do cliente.");
            }
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("O Mercado Pago respondeu " + response.statusCode() + ": " + json.path("message").asText("sem detalhes"));
            }
            return json;
        } catch (IOException exception) {
            throw new IllegalStateException("Sem resposta do Mercado Pago (" + exception.getClass().getSimpleName() + ")");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Consulta ao Mercado Pago interrompida");
        }
    }

    public static boolean isApproved(String status) {
        return "approved".equals(status);
    }
}
