package com.shortlink.util;

import java.net.URI;

public final class UrlValidator {
    private UrlValidator() {
    }

    public static boolean isValidHttpUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && !uri.getHost().trim().isEmpty();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
