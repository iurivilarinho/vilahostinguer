package com.bancada.service;

import com.bancada.models.Customer;
import com.bancada.records.PortalTokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * Signs and checks the customer panel session (HS256 JWT). The key is random, generated once and
 * kept next to the database; the token travels in an httpOnly cookie named {@code token}.
 */
@Service
public class PortalTokenService {

    public static final String COOKIE_NAME = "token";
    private static final String ISSUER = "bancada-portal";
    private static final String VERSION_CLAIM = "ver";
    private static final Duration VALIDITY = Duration.ofDays(7);
    private static final int KEY_BYTES = 64;

    private final SecretKey key;

    public PortalTokenService(@Value("${bancada.data-dir}") String dataDir) {
        this.key = Keys.hmacShaKeyFor(loadOrCreateKey(Paths.get(dataDir, "portal-jwt.key")));
    }

    public String issue(Customer customer) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(ISSUER)
            .subject(String.valueOf(customer.getId()))
            .claim(VERSION_CLAIM, customer.getTokenVersion())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(VALIDITY)))
            .signWith(key)
            .compact();
    }

    public Optional<PortalTokenClaims> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build().parseSignedClaims(token).getPayload();
            Integer version = claims.get(VERSION_CLAIM, Integer.class);
            return Optional.of(new PortalTokenClaims(Long.parseLong(claims.getSubject()), version == null ? -1 : version));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /** httpOnly (JavaScript never sees it), SameSite=Lax (other sites cannot post with it), Secure on HTTPS. */
    public ResponseCookie cookie(String token, boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, token).httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(VALIDITY).build();
    }

    public ResponseCookie clearedCookie(boolean secure) {
        return ResponseCookie.from(COOKIE_NAME, "").httpOnly(true).secure(secure).sameSite("Lax").path("/").maxAge(0).build();
    }

    private static byte[] loadOrCreateKey(Path file) {
        try {
            if (Files.exists(file)) {
                return Files.readAllBytes(file);
            }
            byte[] key = new byte[KEY_BYTES];
            new SecureRandom().nextBytes(key);
            Files.createDirectories(file.getParent());
            Files.write(file, key);
            return key;
        } catch (IOException exception) {
            throw new UncheckedIOException("Não foi possível ler a chave das sessões do painel do cliente", exception);
        }
    }
}
