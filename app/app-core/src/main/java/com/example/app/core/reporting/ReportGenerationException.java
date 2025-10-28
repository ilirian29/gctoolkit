package com.example.app.core.reporting;

/**
 * Checked exception indicating that generating a report failed.
 */
public class ReportGenerationException extends Exception {

    public ReportGenerationException(String message) {
        super(message);
    }

    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
