package com.bancada.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Logs every API request with a request id that is also returned in {@code X-Request-Id}. */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String START_TIME_ATTRIBUTE = "requestStartNanos";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
        response.setHeader(REQUEST_ID_HEADER, requestId);
        LOG.info("[{}] --> {} {}", requestId, request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        Object start = request.getAttribute(START_TIME_ATTRIBUTE);
        long durationMs = start instanceof Long startNanos ? (System.nanoTime() - startNanos) / 1_000_000 : -1;
        LOG.info("[{}] <-- {} {} status={} duration={}ms",
            requestId, request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
    }
}
