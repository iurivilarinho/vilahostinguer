package com.bancada.validation;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalization and validation of DNS names typed by the user. */
public final class Hostnames {

    private static final Pattern HOSTNAME =
        Pattern.compile("(?=.{4,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z][a-z0-9-]{0,61}[a-z0-9]");

    private Hostnames() {
    }

    /** Lowercase, without spaces, scheme, path or trailing dot; null for blank input. */
    public static String normalize(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String result = name.trim().toLowerCase(Locale.ROOT);
        int scheme = result.indexOf("://");
        if (scheme >= 0) {
            result = result.substring(scheme + 3);
        }
        int slash = result.indexOf('/');
        if (slash >= 0) {
            result = result.substring(0, slash);
        }
        return result.endsWith(".") ? result.substring(0, result.length() - 1) : result;
    }

    public static boolean isValid(String name) {
        return name != null && HOSTNAME.matcher(name).matches();
    }
}
