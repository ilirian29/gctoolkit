package com.yourorg.gcdesk;

import java.nio.file.Path;
/**
 * Exception indicating that GC log analysis failed. The exception captures metadata that helps both
 * support staff and the UI surface actionable guidance to the user.
 */
public class AnalysisException extends Exception {

    private final Path logPath;
    private final String collectorType;

    public AnalysisException(String message, Path logPath, String collectorType, Throwable cause) {
        super(message, cause);
        this.logPath = logPath;
        this.collectorType = collectorType;
    }

    public Path getLogPath() {
        return logPath;
    }

    public String getCollectorType() {
        return collectorType;
    }

    /**
     * @return a human-friendly message that can be shown in the UI
     */
    public String getFriendlyMessage() {
        String logLabel = logPath != null ? logPath.getFileName().toString() : "selected log";
        String collectorLabel = collectorType != null && !collectorType.isBlank() ? collectorType : "an unknown collector";
        String baseMessage = getMessage();
        if (baseMessage == null || baseMessage.isBlank()) {
            baseMessage = "We couldn't analyse " + logLabel + " for " + collectorLabel + ".";
        }
        return baseMessage;
    }

    /**
     * @return a single-line description of the failure context suitable for logging.
     */
    public String describeContext() {
        StringBuilder builder = new StringBuilder("logPath=");
        builder.append(logPath != null ? logPath : "<unknown>");
        builder.append(", collectorType=");
        builder.append(collectorType != null ? collectorType : "<unknown>");
        Throwable cause = getCause();
        if (cause != null) {
            builder.append(", cause=").append(cause.getClass().getSimpleName());
            String causeMessage = cause.getMessage();
            if (causeMessage != null && !causeMessage.isBlank()) {
                builder.append('(').append(causeMessage).append(')');
            }
        }
        return builder.toString();
    }
}
