package com.bancada.records;

import java.util.HashMap;
import java.util.Map;

/** The {@code key=value} lines printed by the device scripts, with typed accessors. */
public record KeyValueOutput(Map<String, String> values) {

    public static KeyValueOutput parse(String output) {
        Map<String, String> values = new HashMap<>();
        for (String line : output.split("\n")) {
            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String value = line.substring(separator + 1).trim();
            if (!value.isEmpty()) {
                values.put(line.substring(0, separator).trim(), value);
            }
        }
        return new KeyValueOutput(Map.copyOf(values));
    }

    public String text(String key) {
        return values.get(key);
    }

    public Long longValue(String key) {
        String value = values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public Integer intValue(String key) {
        Long value = longValue(key);
        return value == null ? null : value.intValue();
    }

    public Double doubleValue(String key) {
        String value = values.get(key);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public boolean flag(String key) {
        return "1".equals(values.get(key));
    }
}
