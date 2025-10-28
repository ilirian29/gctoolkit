package com.yourorg.gcdesk.testing;

import com.yourorg.gcdesk.AnalysisException;
import com.yourorg.gcdesk.AnalysisService;
import com.yourorg.gcdesk.model.AnalysisResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test support utilities for working with bundled sample GC logs.
 */
public final class AnalysisFixtures {

    private static final Path RESOURCE_ROOT = Paths.get("..", "..", "resources", "gclogs").toAbsolutePath().normalize();
    private static final Path LARGE_LOG = RESOURCE_ROOT.resolve(Paths.get("large", "gc.log"));
    private static final Path SMALL_LOG = RESOURCE_ROOT.resolve(Paths.get("small", "gc.log"));

    private static final java.util.concurrent.ConcurrentMap<Path, AnalysisResult> CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    private AnalysisFixtures() {
    }

    /**
     * Analyse the bundled sample log and cache the result to avoid repeatedly parsing the
     * relatively large file during the same test run.
     *
     * @return aggregated analysis result for the sample log
     */
    public static AnalysisResult analyseSampleLog() {
        return analyseLargeLog();
    }

    /**
     * Analyse the curated "large" GC log fixture.
     *
     * @return aggregated analysis result for the large fixture
     */
    public static AnalysisResult analyseLargeLog() {
        return analyse(LARGE_LOG);
    }

    /**
     * Analyse the curated "small" GC log fixture.
     *
     * @return aggregated analysis result for the small fixture
     */
    public static AnalysisResult analyseSmallLog() {
        return analyse(SMALL_LOG);
    }

    private static AnalysisResult analyse(Path logPath) {
        Path normalized = logPath.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            throw new IllegalStateException("Missing GC log fixture at " + normalized);
        }

        return CACHE.computeIfAbsent(normalized, AnalysisFixtures::doAnalyse);
    }

    private static AnalysisResult doAnalyse(Path logPath) {
        AnalysisService service = new AnalysisService();
        try {
            return service.analyze(logPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to analyse GC log at " + logPath, e);
        } catch (AnalysisException e) {
            throw new IllegalStateException("Unexpected analysis failure for fixture " + logPath.getFileName(), e);
        }
    }
}
