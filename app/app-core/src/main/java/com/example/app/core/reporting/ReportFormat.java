package com.example.app.core.reporting;

/**
 * Supported output formats for generated reports.
 */
public enum ReportFormat {

    PDF("PDF document", "pdf");

    private final String displayName;
    private final String fileExtension;

    ReportFormat(String displayName, String fileExtension) {
        this.displayName = displayName;
        this.fileExtension = fileExtension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
