package com.example.app.core.logging;

import java.util.Locale;

/**
 * Supported logging profiles mapped to Logback configuration resources.
 */
public enum LoggingProfile {
    DEV("/logging/logback-dev.xml"),
    PROD("/logging/logback-prod.xml");

    private final String configResource;

    LoggingProfile(String configResource) {
        this.configResource = configResource;
    }

    public String getConfigResource() {
        return configResource;
    }

    public static LoggingProfile from(String value, LoggingProfile fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "prod", "production" -> PROD;
            case "dev", "development", "default" -> DEV;
            default -> fallback;
        };
    }
}
