package com.yourorg.gcdesk.testing;

import com.yourorg.gcdesk.AnalysisService;
import com.yourorg.gcdesk.model.AnalysisResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test support utilities for working with bundled sample GC logs.
 */
public final class AnalysisFixtures {

    private static final Path SAMPLE_LOG = Paths.get("..", "..", "gclogs", "samples", "g1-sample.log")
            .toAbsolutePath().normalize();

    private static volatile AnalysisResult cachedResult;

    private AnalysisFixtures() {
    }

    /**
     * Analyse the bundled sample log and cache the result to avoid repeatedly parsing the
     * relatively large file during the same test run.
     *
     * @return aggregated analysis result for the sample log
     */
    public static AnalysisResult analyseSampleLog() {
        AnalysisResult result = cachedResult;
        if (result == null) {
            synchronized (AnalysisFixtures.class) {
                result = cachedResult;
                if (result == null) {
                    cachedResult = result = doAnalyse();
                }
            }
        }
        return result;
    }

    private static AnalysisResult doAnalyse() {
        AnalysisService service = new AnalysisService();
        try {
            return service.analyze(SAMPLE_LOG);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to analyse sample log at " + SAMPLE_LOG, e);
        }
    }
}
