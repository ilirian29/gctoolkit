package com.yourorg.gcdesk;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class CrashReportServiceTest {

    @Test
    void packagesCrashReportWithLogAndMetadata() throws IOException {
        CrashReportService service = new CrashReportService();
        Path workingDir = Files.createTempDirectory("gcdesk-test");
        Path logFile = workingDir.resolve("example.log");
        Files.writeString(logFile, "Example GC log", StandardCharsets.UTF_8);

        AnalysisException exception = new AnalysisException(
                "We couldn't analyse example.log for G1 logs.", logFile, "G1", new RuntimeException("boom"));

        Path outputDir = Files.createTempDirectory("gcdesk-crash-output");
        Path archive = service.packageForSupport(exception, outputDir);

        assertThat(Files.exists(archive)).isTrue();

        Set<String> entryNames = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }
        }

        assertThat(entryNames).anyMatch(name -> name.startsWith("logs/"));
        assertThat(entryNames).contains("metadata/analysis-exception.txt");
        assertThat(entryNames).contains("metadata/system-info.txt");
        assertThat(entryNames).contains("metadata/logging-profile.txt");
    }
}
