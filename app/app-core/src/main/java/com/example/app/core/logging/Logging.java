package com.example.app.core.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Centralized logging facade that configures Logback according to the active profile and
 * exposes SLF4J loggers to the rest of the application.
 */
public final class Logging {

    private static final String PROFILE_PROPERTY = "app.logging.profile";
    private static final String PROFILE_ENV = "APP_LOGGING_PROFILE";

    private static volatile boolean initialized;
    private static volatile LoggingProfile activeProfile = LoggingProfile.DEV;

    private Logging() {
    }

    /**
     * Obtain a logger for the provided type, ensuring the logging system has been configured.
     *
     * @param type class requesting a logger
     * @return configured SLF4J logger
     */
    public static Logger getLogger(Class<?> type) {
        Objects.requireNonNull(type, "type");
        ensureInitialized();
        return LoggerFactory.getLogger(type);
    }

    /**
     * @return the logging profile currently applied to Logback
     */
    public static LoggingProfile getActiveProfile() {
        ensureInitialized();
        return activeProfile;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (Logging.class) {
                if (!initialized) {
                    LoggingProfile profile = resolveProfile();
                    configure(profile);
                    initialized = true;
                }
            }
        }
    }

    private static LoggingProfile resolveProfile() {
        String profileValue = System.getProperty(PROFILE_PROPERTY);
        if (profileValue == null || profileValue.isBlank()) {
            profileValue = Optional.ofNullable(System.getenv(PROFILE_ENV)).orElse(null);
        }
        return LoggingProfile.from(profileValue, LoggingProfile.DEV);
    }

    private static void configure(LoggingProfile profile) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        context.putProperty("gcdesk.logging.profile", profile.name());

        try (InputStream configStream = openConfig(profile)) {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(configStream);
            activeProfile = profile;
        } catch (Exception ex) {
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
            throw new IllegalStateException("Unable to configure logging for profile " + profile, ex);
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    private static InputStream openConfig(LoggingProfile profile) throws IOException {
        InputStream stream = Logging.class.getResourceAsStream(profile.getConfigResource());
        if (stream == null) {
            throw new IOException("Missing Logback configuration: " + profile.getConfigResource());
        }
        return stream;
    }
}
