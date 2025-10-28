package com.yourorg.gcdesk.preferences;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Root preferences model persisted to the user's configuration file.
 */
public record UserPreferences(
        @JsonProperty("theme") Theme theme,
        @JsonProperty("defaultExportDirectory") String defaultExportDirectory,
        @JsonProperty("analysisConcurrency") int analysisConcurrency,
        @JsonProperty("workspace") Workspace workspace) {

    private static final int DEFAULT_MAX_CONCURRENCY = 4;
    private static final int ABSOLUTE_MAX_CONCURRENCY = 64;

    @JsonCreator
    public UserPreferences {
        theme = theme != null ? theme : Theme.SYSTEM;
        defaultExportDirectory = sanitizeExportDirectory(defaultExportDirectory);
        analysisConcurrency = normalizeConcurrency(analysisConcurrency);
        workspace = workspace != null ? workspace : Workspace.empty();
    }

    private static String sanitizeExportDirectory(String directory) {
        if (directory != null && !directory.isBlank()) {
            return directory;
        }
        Path fallback = Paths.get(System.getProperty("user.home"), "gcdesk", "exports");
        return fallback.toString();
    }

    private static int normalizeConcurrency(int value) {
        if (value < 1) {
            return 1;
        }
        return Math.min(value, ABSOLUTE_MAX_CONCURRENCY);
    }

    public static UserPreferences defaults() {
        int processors = Runtime.getRuntime().availableProcessors();
        int concurrency = Math.max(1, Math.min(processors, DEFAULT_MAX_CONCURRENCY));
        return new UserPreferences(Theme.SYSTEM,
                Paths.get(System.getProperty("user.home"), "gcdesk", "exports").toString(),
                concurrency,
                Workspace.empty());
    }

    public Path resolveExportDirectory() {
        return Paths.get(defaultExportDirectory);
    }

    public UserPreferences withTheme(Theme theme) {
        return new UserPreferences(Objects.requireNonNull(theme, "theme"), defaultExportDirectory, analysisConcurrency, workspace);
    }

    public UserPreferences withDefaultExportDirectory(String directory) {
        return new UserPreferences(theme, directory, analysisConcurrency, workspace);
    }

    public UserPreferences withAnalysisConcurrency(int concurrency) {
        return new UserPreferences(theme, defaultExportDirectory, concurrency, workspace);
    }

    public UserPreferences withWorkspace(Workspace workspace) {
        return new UserPreferences(theme, defaultExportDirectory, analysisConcurrency, Objects.requireNonNull(workspace, "workspace"));
    }
}
