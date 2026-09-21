package com.bancada.service;

import com.bancada.exception.TooManyAttemptsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Slows down password guessing on the customer panel: five wrong passwords for the same e-mail, or
 * twenty from the same address, lock that key for fifteen minutes.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_PER_EMAIL = 5;
    private static final int MAX_PER_ADDRESS = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Window> failures = new ConcurrentHashMap<>();

    public void checkAllowed(String email, String address) {
        if (isLocked("email:" + email, MAX_PER_EMAIL) || isLocked("ip:" + address, MAX_PER_ADDRESS)) {
            throw new TooManyAttemptsException("Muitas tentativas erradas. Espere 15 minutos e tente de novo.");
        }
    }

    public void recordFailure(String email, String address) {
        bump("email:" + email);
        bump("ip:" + address);
    }

    public void recordSuccess(String email) {
        failures.remove("email:" + email);
    }

    @Scheduled(fixedDelay = 600_000)
    public void forgetOld() {
        Instant limit = Instant.now().minus(WINDOW);
        failures.values().removeIf(window -> window.start.isBefore(limit));
    }

    private boolean isLocked(String key, int max) {
        Window window = failures.get(key);
        return window != null && window.start.plus(WINDOW).isAfter(Instant.now()) && window.count >= max;
    }

    private void bump(String key) {
        failures.compute(key, (ignored, window) -> {
            Instant now = Instant.now();
            if (window == null || window.start.plus(WINDOW).isBefore(now)) {
                return new Window(now, 1);
            }
            return new Window(window.start, window.count + 1);
        });
    }

    /** Failures counted since {@code start}; private to this service, not part of any contract. */
    private record Window(Instant start, int count) {
    }
}
