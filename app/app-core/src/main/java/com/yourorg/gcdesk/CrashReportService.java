package com.yourorg.gcdesk;

import com.example.app.core.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service that bundles diagnostic data about failed analyses into a single crash report archive.
 */
public class CrashReportService {

    private static final Logger LOGGER = Logging.getLogger(CrashReportService.class);
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    /**
     * Package a crash report into a temporary directory, returning the resulting archive path.
     *
     * @param exception metadata about the analysis failure
     * @return path to the generated crash report archive
     * @throws IOException if writing the archive fails
     */
    public Path packageForSupport(AnalysisException exception) throws IOException {
        Objects.requireNonNull(exception, "exception");
        Path tempDir = Files.createTempDirectory("gcdesk-crash");
        return packageForSupport(exception, tempDir);
    }

    /**
     * Package a crash report into the provided directory, creating it if necessary.
     *
     * @param exception metadata about the analysis failure
     * @param outputDirectory target directory that will hold the archive
     * @return path to the generated crash report archive
     * @throws IOException if writing the archive fails
     */
    public Path packageForSupport(AnalysisException exception, Path outputDirectory) throws IOException {
        Objects.requireNonNull(exception, "exception");
        Objects.requireNonNull(outputDirectory, "outputDirectory");

        Files.createDirectories(outputDirectory);
        String fileName = "gcdesk-crash-" + FILE_NAME_FORMAT.format(Instant.now()) + ".zip";
        Path archive = outputDirectory.resolve(fileName);

        LOGGER.info("Packaging crash report at {}", archive);
        try (ZipOutputStream zipStream = new ZipOutputStream(Files.newOutputStream(archive))) {
            addLogArtifacts(zipStream, exception.getLogPath());
            addTextEntry(zipStream, "metadata/analysis-exception.txt", buildExceptionDetails(exception));
            addTextEntry(zipStream, "metadata/system-info.txt", buildSystemInfo());
            addTextEntry(zipStream, "metadata/logging-profile.txt", Logging.getActiveProfile().name());
        }
        LOGGER.info("Crash report created at {}", archive);
        return archive;
    }

    private void addLogArtifacts(ZipOutputStream stream, Path logPath) throws IOException {
        if (logPath == null) {
            LOGGER.warn("No log path associated with analysis failure; skipping log bundle");
            return;
        }
        if (!Files.exists(logPath)) {
            LOGGER.warn("Log path {} is missing; skipping log bundle", logPath);
            return;
        }
        if (Files.isDirectory(logPath)) {
            try (var paths = Files.walk(logPath)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    try {
                        String relative = logPath.relativize(path).toString().replace('\\', '/');
                        addFileEntry(stream, path, "logs/" + relative);
                    } catch (IOException e) {
                        LOGGER.warn("Unable to add {} to crash report", path, e);
                    }
                });
            }
        } else if (Files.isRegularFile(logPath)) {
            addFileEntry(stream, logPath, "logs/" + logPath.getFileName());
        }
    }

    private void addFileEntry(ZipOutputStream stream, Path source, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        stream.putNextEntry(entry);
        Files.copy(source, stream);
        stream.closeEntry();
    }

    private void addTextEntry(ZipOutputStream stream, String entryName, String contents) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        stream.putNextEntry(entry);
        stream.write(contents.getBytes(StandardCharsets.UTF_8));
        stream.closeEntry();
    }

    private String buildExceptionDetails(AnalysisException exception) {
        StringWriter writer = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            printWriter.println("Friendly message: " + exception.getFriendlyMessage());
            printWriter.println("Log path: " + exception.getLogPath());
            printWriter.println("Collector type: " + exception.getCollectorType());
            printWriter.println("Context: " + exception.describeContext());
            printWriter.println();
            printWriter.println("Stack trace:");
            exception.printStackTrace(printWriter);
        }
        return writer.toString();
    }

    private String buildSystemInfo() {
        Properties properties = System.getProperties();
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        joiner.add("Captured at=" + Instant.now());
        joiner.add("os.name=" + properties.getProperty("os.name"));
        joiner.add("os.version=" + properties.getProperty("os.version"));
        joiner.add("os.arch=" + properties.getProperty("os.arch"));
        joiner.add("java.version=" + properties.getProperty("java.version"));
        joiner.add("user.timezone=" + properties.getProperty("user.timezone"));
        joiner.add("logging.profile=" + Logging.getActiveProfile().name());
        return joiner.toString();
    }
}
