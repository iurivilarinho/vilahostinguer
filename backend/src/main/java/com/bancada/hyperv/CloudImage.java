package com.bancada.hyperv;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Official cloud image of a distribution version and the checksum file published with it. */
public record CloudImage(String url, String checksumUrl, String algorithm) {

    private static final Pattern BSD_LINE = Pattern.compile("^\\w+ \\((.+)\\) = ([0-9a-fA-F]+)$");
    private static final Pattern GNU_LINE = Pattern.compile("^([0-9a-fA-F]+)\\s+\\*?(.+)$");

    public static CloudImage sha256(String url, String checksumUrl) {
        return new CloudImage(url, checksumUrl, "SHA-256");
    }

    public static CloudImage sha512(String url, String checksumUrl) {
        return new CloudImage(url, checksumUrl, "SHA-512");
    }

    public String fileName() {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    /**
     * The expected hash of this image in a checksum file, in either of the two formats the
     * distributions use: {@code <hash>  <file>} (GNU) or {@code SHA256 (<file>) = <hash>} (BSD).
     */
    public String expectedHash(String checksumFile) {
        for (String line : checksumFile.split("\\R")) {
            String trimmed = line.trim();
            Matcher bsd = BSD_LINE.matcher(trimmed);
            if (bsd.matches() && bsd.group(1).equals(fileName())) {
                return bsd.group(2).toLowerCase(Locale.ROOT);
            }
            Matcher gnu = GNU_LINE.matcher(trimmed);
            if (gnu.matches() && gnu.group(2).trim().equals(fileName())) {
                return gnu.group(1).toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }
}
