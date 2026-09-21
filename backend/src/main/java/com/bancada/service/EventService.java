package com.bancada.service;

import com.bancada.enums.DeviceEventType;
import com.bancada.response.DeviceEventResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Pushes device events to the open windows through Server-Sent Events. */
@Service
public class EventService {

    private static final String EVENT_NAME = "device";

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        emitters.add(emitter);
        return emitter;
    }

    public void publish(DeviceEventType type, Long deviceId, String deviceName, Long operationId, String message) {
        DeviceEventResponse event = new DeviceEventResponse(type, deviceId, deviceName, operationId, message, LocalDateTime.now());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(EVENT_NAME).data(event, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException exception) {
                emitters.remove(emitter);
            }
        }
    }

    /** Keeps idle connections alive through proxies and lets dead ones be noticed. */
    @Scheduled(fixedDelay = 20_000)
    public void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException exception) {
                emitters.remove(emitter);
            }
        }
    }
}
