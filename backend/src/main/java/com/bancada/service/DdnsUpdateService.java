package com.bancada.service;

import com.bancada.models.Domain;
import com.bancada.records.DdnsUpdateResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

/** Pushes the public IP to the DNS provider of a domain. Failures come back as {@link IllegalStateException}. */
@Service
public class DdnsUpdateService {

    private static final String CLOUDFLARE_API = "https://api.cloudflare.com/client/v4";
    private static final String DUCKDNS_SUFFIX = ".duckdns.org";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final int CLOUDFLARE_TTL_SECONDS = 120;
    private static final int MESSAGE_PREVIEW = 160;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DdnsUpdateService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public DdnsUpdateResult update(Domain domain, String secret, String ip) {
        return switch (domain.getProvider()) {
            case CLOUDFLARE -> cloudflare(domain, secret, ip);
            case DUCKDNS -> duckDns(domain.getName(), secret, ip);
            case CUSTOM_URL -> customUrl(secret, ip);
            case MANUAL -> new DdnsUpdateResult("Sem atualização automática: o registro é mantido no provedor.", null);
        };
    }

    /** DuckDNS subdomain of a name: "casa" for casa.duckdns.org and for blog.casa.duckdns.org. */
    static String duckDnsSubdomain(String name) {
        if (!name.endsWith(DUCKDNS_SUFFIX)) {
            throw new IllegalArgumentException("Um domínio DuckDNS termina em " + DUCKDNS_SUFFIX);
        }
        String prefix = name.substring(0, name.length() - DUCKDNS_SUFFIX.length());
        return prefix.substring(prefix.lastIndexOf('.') + 1);
    }

    private DdnsUpdateResult duckDns(String name, String token, String ip) {
        String url = "https://www.duckdns.org/update?domains=" + encode(duckDnsSubdomain(name)) + "&token=" + encode(token)
            + "&ip=" + encode(ip);
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(url)).GET());
        String body = response.body() == null ? "" : response.body().trim();
        if (response.statusCode() != 200 || !body.startsWith("OK")) {
            throw new IllegalStateException("O DuckDNS recusou a atualização (" + preview(body)
                + "). Confira o token e se o subdomínio é da sua conta.");
        }
        return new DdnsUpdateResult("DuckDNS apontado para " + ip + ".", null);
    }

    private DdnsUpdateResult customUrl(String template, String ip) {
        String url = template.trim().replace("{ip}", encode(ip));
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            throw new IllegalStateException("A URL de atualização precisa começar com http:// ou https://");
        }
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(url)).GET());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("A URL de atualização respondeu " + response.statusCode() + ": " + preview(response.body()));
        }
        return new DdnsUpdateResult("Atualizado para " + ip + ". Resposta: " + preview(response.body()), null);
    }

    private DdnsUpdateResult cloudflare(Domain domain, String token, String ip) {
        String zoneId = domain.getZoneId() != null ? domain.getZoneId() : findCloudflareZone(domain.getName(), token);
        List<String> names = new ArrayList<>(List.of(domain.getName()));
        if (domain.isWildcard()) {
            names.add("*." + domain.getName());
        }
        boolean proxied = false;
        for (String name : names) {
            proxied |= upsertCloudflareRecord(zoneId, name, ip, token);
        }
        String message = "Registro A de " + String.join(" e ", names) + " apontado para " + ip + ".";
        if (proxied) {
            message += " O proxy da Cloudflare está ligado: sites HTTP/HTTPS passam, portas TCP (SSH e outras) não.";
        }
        return new DdnsUpdateResult(message, zoneId);
    }

    /** Tries each parent of the name, longest first: blog.casa.exemplo.com.br, casa.exemplo.com.br, exemplo.com.br... */
    private String findCloudflareZone(String name, String token) {
        String[] labels = name.split("\\.");
        for (int start = 0; start < labels.length - 1; start++) {
            String candidate = String.join(".", Arrays.copyOfRange(labels, start, labels.length));
            JsonNode result = cloudflare("GET", "/zones?name=" + encode(candidate), token, null);
            if (result.isArray() && !result.isEmpty()) {
                return result.get(0).path("id").asText();
            }
        }
        throw new IllegalStateException("Nenhuma zona da Cloudflare cobre " + name
            + ". O domínio precisa estar na conta e o token precisa da permissão Zone → DNS → Edit.");
    }

    /** Creates or updates the A record; returns whether the Cloudflare proxy is on for it. */
    private boolean upsertCloudflareRecord(String zoneId, String name, String ip, String token) {
        JsonNode records = cloudflare("GET", "/zones/" + zoneId + "/dns_records?type=A&name=" + encode(name), token, null);
        if (records.isArray() && !records.isEmpty()) {
            JsonNode record = records.get(0);
            if (!ip.equals(record.path("content").asText())) {
                ObjectNode body = objectMapper.createObjectNode().put("content", ip);
                cloudflare("PATCH", "/zones/" + zoneId + "/dns_records/" + record.path("id").asText(), token, body);
            }
            return record.path("proxied").asBoolean(false);
        }
        ObjectNode body = objectMapper.createObjectNode()
            .put("type", "A")
            .put("name", name)
            .put("content", ip)
            .put("ttl", CLOUDFLARE_TTL_SECONDS)
            .put("proxied", false);
        cloudflare("POST", "/zones/" + zoneId + "/dns_records", token, body);
        return false;
    }

    private JsonNode cloudflare(String method, String path, String token, JsonNode body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(CLOUDFLARE_API + path))
            .header("Authorization", "Bearer " + token.trim())
            .header("Content-Type", "application/json")
            .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body.toString()));
        HttpResponse<String> response = send(request);
        JsonNode json;
        try {
            json = objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("A Cloudflare respondeu " + response.statusCode() + " sem JSON");
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new IllegalStateException("A Cloudflare recusou o token (" + cloudflareErrors(json)
                + "). Use um API Token com Zone → DNS → Edit.");
        }
        if (!json.path("success").asBoolean(false)) {
            throw new IllegalStateException("A Cloudflare recusou o pedido: " + cloudflareErrors(json));
        }
        return json.path("result");
    }

    private static String cloudflareErrors(JsonNode json) {
        List<String> messages = new ArrayList<>();
        json.path("errors").forEach(error -> messages.add(error.path("message").asText()));
        return messages.isEmpty() ? "sem detalhes" : String.join("; ", messages);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        try {
            return httpClient.send(request.timeout(REQUEST_TIMEOUT).build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new IllegalStateException("Sem resposta de " + request.build().uri().getHost() + " ("
                + exception.getClass().getSimpleName() + ")");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Atualização interrompida");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String preview(String text) {
        if (text == null || text.isBlank()) {
            return "vazia";
        }
        String single = text.trim().replaceAll("\\s+", " ");
        return single.length() <= MESSAGE_PREVIEW ? single : single.substring(0, MESSAGE_PREVIEW) + "…";
    }
}
