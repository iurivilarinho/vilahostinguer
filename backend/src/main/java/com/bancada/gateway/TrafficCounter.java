package com.bancada.gateway;

import com.bancada.response.RouteTrafficResponse;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Live counters of one route, kept in memory while the panel runs. */
public final class TrafficCounter {

    private final AtomicInteger active = new AtomicInteger();
    private final AtomicLong total = new AtomicLong();
    private final AtomicLong bytesIn = new AtomicLong();
    private final AtomicLong bytesOut = new AtomicLong();
    private volatile LocalDateTime lastConnectionAt;
    private volatile String lastError;
    private volatile LocalDateTime lastErrorAt;

    public void opened() {
        active.incrementAndGet();
        total.incrementAndGet();
        lastConnectionAt = LocalDateTime.now();
    }

    public void closed() {
        active.decrementAndGet();
    }

    public void addIn(long bytes) {
        bytesIn.addAndGet(bytes);
    }

    public void addOut(long bytes) {
        bytesOut.addAndGet(bytes);
    }

    public void error(String message) {
        lastError = message;
        lastErrorAt = LocalDateTime.now();
    }

    public RouteTrafficResponse toResponse(Long routeId) {
        return new RouteTrafficResponse(routeId, active.get(), total.get(), bytesIn.get(), bytesOut.get(), lastConnectionAt,
            lastError, lastErrorAt);
    }
}
