package com.yourorg.gcdesk;

import com.microsoft.gctoolkit.event.GCCause;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.yourorg.gcdesk.model.AnalysisResult;
import com.yourorg.gcdesk.model.CollectionCycleSummary;
import com.yourorg.gcdesk.model.GCCauseSummary;
import com.yourorg.gcdesk.model.HeapOccupancySummary;
import com.yourorg.gcdesk.model.PauseStatistics;
import com.yourorg.gcdesk.testing.AnalysisFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServiceIntegrationTest {

    @Test
    void pauseStatisticsAreAggregatedFromSampleLog() {
        AnalysisResult result = AnalysisFixtures.analyseSampleLog();
        PauseStatistics stats = result.getPauseStatistics();

        assertThat(stats.getTotalPauseTime())
                .as("total pause time")
                .isGreaterThan(0.0d);
        assertThat(stats.getPercentPaused())
                .as("percent paused")
                .isGreaterThan(0.0d);
        assertThat(stats.getMaxPause())
                .as("max pause")
                .isGreaterThan(stats.getAveragePause());
    }

    @Test
    void heapOccupancySeriesRetainsPauseYoungPoints() {
        AnalysisResult result = AnalysisFixtures.analyseSampleLog();
        HeapOccupancySummary heapSummary = result.getHeapOccupancySummary();

        Map<GarbageCollectionTypes, List<HeapOccupancySummary.XYPoint>> series = heapSummary.getSeriesByType();
        assertThat(series)
                .as("heap occupancy series")
                .isNotEmpty();

        GarbageCollectionTypes youngCollectionType = series.keySet().stream()
                .filter(type -> type.getLabel().toLowerCase(java.util.Locale.ROOT).contains("young"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected at least one Young GC series"));

        assertThat(series.get(youngCollectionType))
                .as("data points for Young GC collections")
                .isNotEmpty();
    }

    @Test
    void collectionCycleCountsReflectYoungPauses() {
        AnalysisResult result = AnalysisFixtures.analyseSampleLog();
        CollectionCycleSummary cycles = result.getCollectionCycleSummary();

        Map<GarbageCollectionTypes, Integer> counts = cycles.getCounts();
        assertThat(counts)
                .as("cycle counts")
                .isNotEmpty();

        GarbageCollectionTypes youngCollectionType = counts.keySet().stream()
                .filter(type -> type.getLabel().toLowerCase(java.util.Locale.ROOT).contains("young"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected at least one Young GC cycle"));

        assertThat(counts.get(youngCollectionType))
                .as("Young cycle count")
                .isGreaterThan(0);
    }

    @Test
    void gcCauseSummaryCapturesDominantCause() {
        AnalysisResult result = AnalysisFixtures.analyseSampleLog();
        GCCauseSummary causes = result.getGcCauseSummary();

        assertThat(causes.getCauseCounts())
                .as("cause counts")
                .isNotEmpty();

        causes.getCauseCounts().forEach((cause, count) -> {
            assertThat(count)
                    .as("event count for cause %s", cause)
                    .isGreaterThan(0);
            assertThat(causes.getAverageDurations())
                    .as("average duration map contains %s", cause)
                    .containsKey(cause);
        });
    }
}
