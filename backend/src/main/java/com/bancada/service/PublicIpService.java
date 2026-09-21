package com.bancada.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Public IPv4 of this network, as the internet sees it. Asked to more than one service, because any
 * of them can be down; the answer is kept for a minute so the status page and the DDNS share it.
 */
@Service
public class PublicIpService {

    private static final List<String> SOURCES = List.of("https://api.ipify.org", "https://ipv4.icanhazip.com", "https://ifconfig.me/ip");
    private static final Pattern IPV4 = Pattern.compile("(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}");
    private static final Duration CACHE = Duration.ofSeconds(55);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient;
    private volatile String ip;
    private volatile String lastKnownIp;
    private volatile Instant checkedAt;
    private volatile String error;

    public PublicIpService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /** The current public IP; throws {@link IllegalStateException} when no source answers. */
    public synchronized String current() {
        if (checkedAt != null && checkedAt.plus(CACHE).isAfter(Instant.now())) {
            if (ip == null) {
                throw new IllegalStateException(error);
            }
            return ip;
        }
        StringBuilder failures = new StringBuilder();
        for (String source : SOURCES) {
            try {
                HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(source)).timeout(REQUEST_TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
                String body = response.body() == null ? "" : response.body().trim();
                if (response.statusCode() == 200 && IPV4.matcher(body).matches()) {
                    remember(body, null);
                    return body;
                }
                failures.append(source).append(": resposta inesperada; ");
            } catch (IOException exception) {
                failures.append(source).append(": ").append(exception.getClass().getSimpleName()).append("; ");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Consulta do IP público interrompida");
            }
        }
        remember(null, "Não foi possível descobrir o IP público (sem internet?). " + failures.toString().trim());
        throw new IllegalStateException(error);
    }

    /** Whether the next {@link #current()} goes to the internet. */
    public boolean isStale() {
        Instant checked = checkedAt;
        return checked == null || checked.plus(CACHE).isBefore(Instant.now());
    }

    /** Last known IP without going to the internet; null when never found. */
    public String lastKnown() {
        return lastKnownIp;
    }

    public String lastError() {
        return error;
    }

    private void remember(String ip, String error) {
        this.ip = ip;
        if (ip != null) {
            this.lastKnownIp = ip;
        }
        this.error = error;
        this.checkedAt = Instant.now();
    }
}
