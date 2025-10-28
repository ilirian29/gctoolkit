package com.yourorg.gcdesk.preferences;

import com.example.app.core.logging.Logging;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;

/**
 * Service responsible for loading and persisting {@link UserPreferences} with schema validation.
 */
public class PreferencesService {

    private static final Logger LOGGER = Logging.getLogger(PreferencesService.class);
    private static final String SCHEMA_RESOURCE = "/com/yourorg/gcdesk/preferences/preferences-schema.json";

    private final Path configPath;
    private final ObjectMapper objectMapper;
    private final Schema schema;

    public PreferencesService() {
        this(resolveConfigPath());
    }

    public PreferencesService(Path configPath) {
        this.configPath = Objects.requireNonNull(configPath, "configPath");
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
        this.schema = loadSchema();
    }

    private static Path resolveConfigPath() {
        return defaultConfigDirectory().resolve("config.json");
    }

    public static Path defaultConfigDirectory() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, "GCDesk");
            }
        } else if (osName.contains("mac")) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support", "GCDesk");
        }
        return Paths.get(System.getProperty("user.home"), ".gcdesk");
    }

    /**
     * Load preferences from disk, creating a default file if none exists.
     *
     * @return resolved preferences
     */
    public synchronized UserPreferences load() {
        if (!Files.exists(configPath)) {
            UserPreferences defaults = createDefaults();
            save(defaults);
            return defaults;
        }
        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JSONObject document = new JSONObject(new JSONTokener(json));
            schema.validate(document);
            return objectMapper.readValue(json, UserPreferences.class);
        } catch (ValidationException validationException) {
            LOGGER.warn("Preferences at {} failed validation. Recreating defaults.", configPath, validationException);
            UserPreferences defaults = createDefaults();
            save(defaults);
            return defaults;
        } catch (IOException ex) {
            throw new PreferencesException("Failed to read preferences from " + configPath, ex);
        }
    }

    /**
     * Persist the supplied preferences to disk after validating against the JSON schema.
     *
     * @param preferences preferences to persist
     * @return the persisted preferences
     */
    public synchronized UserPreferences save(UserPreferences preferences) {
        Objects.requireNonNull(preferences, "preferences");
        try {
            ensureParentDirectory();
            ensureExportDirectory(preferences);
            String json = objectMapper.writeValueAsString(preferences);
            JSONObject document = new JSONObject(new JSONTokener(json));
            schema.validate(document);
            Files.writeString(configPath, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return preferences;
        } catch (ValidationException ex) {
            throw new PreferencesException("Preferences do not match the required schema", ex);
        } catch (JsonProcessingException ex) {
            throw new PreferencesException("Unable to serialise preferences", ex);
        } catch (IOException ex) {
            throw new PreferencesException("Failed to write preferences to " + configPath, ex);
        }
    }

    public Path getConfigPath() {
        return configPath;
    }

    public Path getConfigDirectory() {
        Path parent = configPath.getParent();
        return parent != null ? parent : defaultConfigDirectory();
    }

    private void ensureParentDirectory() throws IOException {
        Path parent = configPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private Schema loadSchema() {
        try (InputStream inputStream = PreferencesService.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing preferences schema: " + SCHEMA_RESOURCE);
            }
            JSONObject schemaJson = new JSONObject(new JSONTokener(inputStream));
            SchemaLoader loader = SchemaLoader.builder()
                    .schemaJson(schemaJson)
                    .draft202012Support()
                    .build();
            return loader.load().build();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read preferences schema", ex);
        }
    }

    private UserPreferences createDefaults() {
        UserPreferences defaults = UserPreferences.defaults();
        try {
            ensureParentDirectory();
            ensureExportDirectory(defaults);
        } catch (IOException ex) {
            LOGGER.warn("Unable to provision default directories", ex);
        }
        return defaults;
    }

    private void ensureExportDirectory(UserPreferences preferences) throws IOException {
        Path exportDirectory = preferences.resolveExportDirectory();
        if (!Files.exists(exportDirectory)) {
            Files.createDirectories(exportDirectory);
        }
    }
}
