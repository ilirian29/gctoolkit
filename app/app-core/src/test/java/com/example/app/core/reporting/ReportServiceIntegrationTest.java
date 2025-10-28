package com.example.app.core.reporting;

import com.yourorg.gcdesk.model.AnalysisResult;
import com.yourorg.gcdesk.testing.AnalysisFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceIntegrationTest {

    private final ReportService reportService = new ReportService();

    @TempDir
    Path exportDirectory;

    @Test
    void htmlReportIncludesKeySectionsAndEmbeddedCharts() throws Exception {
        AnalysisResult result = AnalysisFixtures.analyseLargeLog();

        Path report = reportService.generateReport(result, exportDirectory, ReportFormat.HTML);

        assertThat(report).exists();
        assertThat(report.getParent()).isEqualTo(exportDirectory);
        assertThat(report.getFileName().toString()).endsWith(".html");

        String html = Files.readString(report);
        assertThat(html).contains("GC Analysis Report");
        assertThat(html).contains("Key metrics");
        assertThat(html).contains("Total pause time");
        assertThat(html).contains("Recommendations");

        String totalPause = String.format(Locale.ROOT, "%.2f", result.getPauseStatistics().getTotalPauseTime());
        String percentPaused = String.format(Locale.ROOT, "%.2f", result.getPauseStatistics().getPercentPaused());
        assertThat(html).contains(totalPause + " ms");
        assertThat(html).contains(percentPaused + " %");

        assertThat(html).contains("data:image/png;base64,");
        assertThat(html).contains("<li><strong");
    }

    @Test
    void pdfReportIsWrittenToExportDirectory() throws Exception {
        AnalysisResult result = AnalysisFixtures.analyseSmallLog();

        Path report = reportService.generateReport(result, exportDirectory, ReportFormat.PDF);

        assertThat(report).exists();
        assertThat(report.getParent()).isEqualTo(exportDirectory);
        assertThat(report.getFileName().toString()).endsWith(".pdf");

        String sanitizedBase = result.getSource().getFileName().toString().replaceAll("[^a-zA-Z0-9-_]", "_");
        assertThat(report.getFileName().toString()).startsWith(sanitizedBase + "-");
        assertThat(Files.size(report)).isGreaterThan(0L);
    }
}
