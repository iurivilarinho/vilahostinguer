package com.bancada.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Loads the shell scripts shipped in {@code resources/scripts}. */
@Service
public class DeviceScriptService {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String load(String name) {
        return cache.computeIfAbsent(name, key -> {
            ClassPathResource resource = new ClassPathResource("scripts/" + key + ".sh");
            try (InputStream input = resource.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            } catch (IOException exception) {
                throw new IllegalStateException("Script interno ausente: " + key, exception);
            }
        });
    }
}
