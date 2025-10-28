package com.example.app.core.reporting;

import com.microsoft.gctoolkit.event.GCCause;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.yourorg.gcdesk.model.AnalysisResult;
import com.yourorg.gcdesk.model.CollectionCycleSummary;
import com.yourorg.gcdesk.model.GCCauseSummary;
import com.yourorg.gcdesk.model.HeapOccupancySummary;
import com.yourorg.gcdesk.model.PauseStatistics;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Translates analysed aggregation data into the view model used by FreeMarker templates.
 */
class ReportViewModelMapper {

    private final ReportChartGenerator chartGenerator = new ReportChartGenerator();
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault());

    ReportViewModel map(AnalysisResult result, Instant generatedAt) throws ReportGenerationException {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(generatedAt, "generatedAt");

        PauseStatistics pauseStatistics = result.getPauseStatistics();
        ReportViewModel.SummaryMetrics summaryMetrics = new ReportViewModel.SummaryMetrics(
                pauseStatistics.getTotalPauseTime(),
                pauseStatistics.getPercentPaused(),
                pauseStatistics.getAveragePause(),
                pauseStatistics.getMedianPause(),
                pauseStatistics.getP90Pause(),
                pauseStatistics.getP99Pause(),
                pauseStatistics.getMaxPause());

        List<ReportViewModel.ChartViewModel> charts = buildCharts(result.getHeapOccupancySummary(),
                result.getGcCauseSummary());

        List<ReportViewModel.RecommendationViewModel> recommendations = buildRecommendations(result);

        List<ReportViewModel.CauseRowViewModel> causeRows = buildCauseRows(result.getGcCauseSummary());
        List<ReportViewModel.CycleRowViewModel> cycleRows = buildCycleRows(result.getCollectionCycleSummary());

        Path source = result.getSource();
        String generatedAtText = timestampFormatter.format(generatedAt);
        String runtimeSummary = buildRuntimeSummary(result);

        return new ReportViewModel(
                "GC Analysis Report",
                source.toString(),
                generatedAtText,
                summaryMetrics,
                charts,
                recommendations,
                causeRows,
                cycleRows,
                runtimeSummary);
    }

    private List<ReportViewModel.ChartViewModel> buildCharts(HeapOccupancySummary heapSummary,
                                                             GCCauseSummary causeSummary)
            throws ReportGenerationException {
        List<ReportViewModel.ChartViewModel> charts = new ArrayList<>();
        chartGenerator.renderHeapOccupancyChart(heapSummary).ifPresent(uri ->
                charts.add(new ReportViewModel.ChartViewModel("Heap occupancy trend",
                        "Heap usage after each collection cycle.", uri)));

        chartGenerator.renderCauseDistributionChart(causeSummary).ifPresent(uri ->
                charts.add(new ReportViewModel.ChartViewModel("GC cause distribution",
                        "Relative frequency of GC causes observed in the log.", uri)));
        return charts;
    }

    private List<ReportViewModel.RecommendationViewModel> buildRecommendations(AnalysisResult result) {
        List<ReportViewModel.RecommendationViewModel> recommendations = new ArrayList<>();

        PauseStatistics pauseStats = result.getPauseStatistics();
        if (pauseStats.getPercentPaused() > 5.0d) {
            recommendations.add(new ReportViewModel.RecommendationViewModel(
                    "Investigate pause ratio",
                    String.format(Locale.getDefault(),
                            "The application spent %.2f%% of wall-clock time paused. Consider tuning GC threads or " +
                                    "adjusting heap sizing.", pauseStats.getPercentPaused())));
        }
        if (pauseStats.getMaxPause() > 1000.0d) {
            recommendations.add(new ReportViewModel.RecommendationViewModel(
                    "Long pauses detected",
                    String.format(Locale.getDefault(),
                            "The longest pause lasted %.2f ms. Review slow phases such as concurrent mark " +
                                    "or large object allocations.", pauseStats.getMaxPause())));
        }
        if (pauseStats.getAveragePause() > 500.0d) {
            recommendations.add(new ReportViewModel.RecommendationViewModel(
                    "High average pause time",
                    String.format(Locale.getDefault(),
                            "Average pause time is %.2f ms which may impact latency-sensitive workloads.",
                            pauseStats.getAveragePause())));
        }

        GCCauseSummary causeSummary = result.getGcCauseSummary();
        Optional<Map.Entry<GCCause, Integer>> topCause = causeSummary.getCauseCounts().entrySet().stream()
                .max(Map.Entry.comparingByValue());
        topCause.ifPresent(entry -> recommendations.add(new ReportViewModel.RecommendationViewModel(
                "Dominant GC trigger",
                String.format(Locale.getDefault(),
                        "%s accounted for %d events. Investigate why this cause is dominant.",
                        entry.getKey().getLabel(), entry.getValue()))));

        if (recommendations.isEmpty()) {
            recommendations.add(new ReportViewModel.RecommendationViewModel(
                    "No critical findings",
                    "No obvious anomalies were detected in the analysed log. Continue monitoring to " +
                            "establish long-term trends."));
        }

        return recommendations;
    }

    private List<ReportViewModel.CauseRowViewModel> buildCauseRows(GCCauseSummary summary) {
        Map<GCCause, Integer> counts = summary.getCauseCounts();
        Map<GCCause, Double> averages = summary.getAverageDurations();
        return counts.entrySet().stream()
                .sorted(Map.Entry.<GCCause, Integer>comparingByValue().reversed())
                .map(entry -> new ReportViewModel.CauseRowViewModel(entry.getKey().getLabel(), entry.getValue(),
                        averages.getOrDefault(entry.getKey(), 0.0d)))
                .toList();
    }

    private List<ReportViewModel.CycleRowViewModel> buildCycleRows(CollectionCycleSummary summary) {
        Map<GarbageCollectionTypes, Integer> counts = summary.getCounts();
        return counts.entrySet().stream()
                .sorted(Map.Entry.<GarbageCollectionTypes, Integer>comparingByValue().reversed())
                .map(entry -> new ReportViewModel.CycleRowViewModel(entry.getKey().getLabel(), entry.getValue()))
                .toList();
    }

    private String buildRuntimeSummary(AnalysisResult result) {
        int totalEvents = result.getGcCauseSummary().getCauseCounts().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        String analyzedAt = timestampFormatter.format(result.getAnalyzedAt());
        return String.format(Locale.getDefault(),
                "Analysis executed at %s capturing %,d GC cause events across %,d GC cycle types.",
                analyzedAt,
                totalEvents,
                result.getCollectionCycleSummary().getCounts().size());
    }
}
