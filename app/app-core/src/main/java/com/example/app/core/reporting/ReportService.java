package com.example.app.core.reporting;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.yourorg.gcdesk.model.AnalysisResult;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * High level façade for generating PDF reports from analysed GC data.
 */
public class ReportService {

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    private final ReportViewModelMapper viewModelMapper;
    private final ReportTemplateRenderer templateRenderer;

    public ReportService() {
        this(new ReportViewModelMapper(), new ReportTemplateRenderer());
    }

    ReportService(ReportViewModelMapper viewModelMapper, ReportTemplateRenderer templateRenderer) {
        this.viewModelMapper = Objects.requireNonNull(viewModelMapper, "viewModelMapper");
        this.templateRenderer = Objects.requireNonNull(templateRenderer, "templateRenderer");
    }

    /**
     * Generate a report in the specified format inside the provided output directory.
     *
     * @param result          analysed GC data
     * @param outputDirectory directory where the report should be written
     * @param format          target report format
     * @return path to the generated report file
     * @throws ReportGenerationException if rendering fails
     */
    public Path generateReport(AnalysisResult result, Path outputDirectory, ReportFormat format)
            throws ReportGenerationException {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Objects.requireNonNull(format, "format");

        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            throw new ReportGenerationException("Unable to create output directory: " + outputDirectory, e);
        }

        Instant generatedAt = Instant.now();
        ReportViewModel viewModel = viewModelMapper.map(result, generatedAt);
        String html = templateRenderer.render(viewModel);

        String fileName = buildFileName(result, generatedAt, format);
        Path outputFile = outputDirectory.resolve(fileName);

        switch (format) {
            case PDF -> renderPdf(html, outputFile);
            default -> throw new ReportGenerationException("Unsupported format: " + format);
        }

        return outputFile;
    }

    private void renderPdf(String html, Path outputFile) throws ReportGenerationException {
        try (OutputStream outputStream = Files.newOutputStream(outputFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.useFastMode();
            builder.run();
        } catch (Exception e) {
            throw new ReportGenerationException("Unable to render PDF report", e);
        }
    }

    private String buildFileName(AnalysisResult result, Instant generatedAt, ReportFormat format) {
        String baseName = result.getSource().getFileName() != null
                ? result.getSource().getFileName().toString()
                : "gc-report";
        baseName = baseName.replaceAll("[^a-zA-Z0-9-_]", "_");
        String timestamp = FILE_TIMESTAMP_FORMAT.format(generatedAt);
        return String.format(Locale.ROOT, "%s-%s.%s", baseName, timestamp, format.getFileExtension());
    }
}
