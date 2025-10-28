package com.yourorg.gcdesk;

import com.example.app.core.aggregations.CollectionCycleCountsSummary;
import com.example.app.core.aggregations.DesktopGCCauseFrequencySummary;
import com.example.app.core.aggregations.DesktopPausePercentileSummary;
import com.example.app.core.aggregations.HeapOccupancyAfterCollectionSummary;
import com.example.app.core.aggregations.PauseTimeSummary;
import com.example.app.core.collections.XYDataSet;
import com.microsoft.gctoolkit.GCToolKit;
import com.microsoft.gctoolkit.io.GCLogFile;
import com.microsoft.gctoolkit.io.RotatingGCLogFile;
import com.microsoft.gctoolkit.io.SingleGCLogFile;
import com.microsoft.gctoolkit.jvm.JavaVirtualMachine;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.yourorg.gcdesk.model.AnalysisResult;
import com.yourorg.gcdesk.model.CollectionCycleSummary;
import com.yourorg.gcdesk.model.GCCauseSummary;
import com.yourorg.gcdesk.model.HeapOccupancySummary;
import com.yourorg.gcdesk.model.HeapOccupancySummary.XYPoint;
import com.yourorg.gcdesk.model.PauseStatistics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service responsible for analysing GC logs and caching the resulting aggregations.
 */
public class AnalysisService {

    private final Map<UUID, AnalysisResult> analysisCache = new ConcurrentHashMap<>();
    private final Map<Path, UUID> pathIndex = new ConcurrentHashMap<>();

    /**
     * Analyse the supplied GC log and return the aggregated results. If the log has been processed
     * previously, the cached result will be returned to avoid reprocessing the log.
     *
     * @param logFilePath path to the GC log file or directory
     * @return aggregated analysis result
     * @throws IOException if reading the log fails
     */
    public AnalysisResult analyze(Path logFilePath) throws IOException {
        Objects.requireNonNull(logFilePath, "logFilePath");
        Path normalized = logFilePath.toAbsolutePath().normalize();

        UUID cachedId = pathIndex.get(normalized);
        if (cachedId != null) {
            AnalysisResult cachedResult = analysisCache.get(cachedId);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        GCLogFile logFile = createLogFile(normalized);
        GCToolKit toolKit = new GCToolKit();
        toolKit.loadAggregationsFromServiceLoader();
        JavaVirtualMachine machine = toolKit.analyze(logFile);

        UUID analysisId = UUID.randomUUID();
        AnalysisResult result = buildAnalysisResult(analysisId, normalized, machine);
        analysisCache.put(analysisId, result);
        pathIndex.put(normalized, analysisId);
        return result;
    }

    /**
     * Retrieve a previously computed analysis result by its identifier.
     *
     * @param analysisId identifier returned from {@link #analyze(Path)}
     * @return optional containing the cached result if present
     */
    public Optional<AnalysisResult> get(UUID analysisId) {
        return Optional.ofNullable(analysisCache.get(analysisId));
    }

    private GCLogFile createLogFile(Path path) {
        if (Files.isDirectory(path)) {
            return new RotatingGCLogFile(path);
        }
        return new SingleGCLogFile(path);
    }

    private AnalysisResult buildAnalysisResult(UUID analysisId, Path source, JavaVirtualMachine machine) {
        HeapOccupancySummary heapOccupancySummary = machine.getAggregation(HeapOccupancyAfterCollectionSummary.class)
                .filter(aggregation -> !aggregation.isEmpty())
                .map(HeapOccupancyAfterCollectionSummary::get)
                .map(this::toHeapOccupancySummary)
                .orElseGet(HeapOccupancySummary::empty);

        PauseTimeSummary pauseTimeSummary = machine.getAggregation(PauseTimeSummary.class).orElse(null);
        DesktopPausePercentileSummary percentileSummary = machine.getAggregation(DesktopPausePercentileSummary.class)
                .orElse(null);
        PauseStatistics pauseStatistics = toPauseStatistics(pauseTimeSummary, percentileSummary);

        CollectionCycleSummary collectionCycleSummary = machine.getAggregation(CollectionCycleCountsSummary.class)
                .filter(aggregation -> !aggregation.isEmpty())
                .map(CollectionCycleCountsSummary::getCounts)
                .map(CollectionCycleSummary::new)
                .orElseGet(CollectionCycleSummary::empty);

        GCCauseSummary gcCauseSummary = machine.getAggregation(DesktopGCCauseFrequencySummary.class)
                .filter(aggregation -> !aggregation.isEmpty())
                .map(summary -> new GCCauseSummary(summary.getCauseCounts(),
                        summary.getAverageDurationsByCause(), summary.getTypeBreakdown()))
                .orElseGet(GCCauseSummary::empty);

        return new AnalysisResult(analysisId, source, Instant.now(), heapOccupancySummary, pauseStatistics,
                collectionCycleSummary, gcCauseSummary);
    }

    private HeapOccupancySummary toHeapOccupancySummary(Map<GarbageCollectionTypes, XYDataSet> source) {
        Map<GarbageCollectionTypes, List<XYPoint>> series = new EnumMap<>(GarbageCollectionTypes.class);
        for (Map.Entry<GarbageCollectionTypes, XYDataSet> entry : source.entrySet()) {
            XYDataSet dataSet = entry.getValue();
            List<XYPoint> points = dataSet.getItems().stream()
                    .map(point -> new XYPoint(point.getX().doubleValue(), point.getY().doubleValue()))
                    .collect(Collectors.toUnmodifiableList());
            series.put(entry.getKey(), points);
        }
        return new HeapOccupancySummary(series);
    }

    private PauseStatistics toPauseStatistics(PauseTimeSummary totals, DesktopPausePercentileSummary percentiles) {
        if (totals == null && percentiles == null) {
            return PauseStatistics.empty();
        }
        double totalPauseTime = totals != null ? totals.getTotalPauseTime() : 0.0d;
        double percentPaused = totals != null ? totals.getPercentPaused() : 0.0d;
        double averagePause = percentiles != null ? percentiles.getAveragePause() : 0.0d;
        double medianPause = percentiles != null ? percentiles.getMedianPause() : 0.0d;
        double p90Pause = percentiles != null ? percentiles.getP90Pause() : 0.0d;
        double p99Pause = percentiles != null ? percentiles.getP99Pause() : 0.0d;
        double maxPause = percentiles != null ? percentiles.getMaxPause().orElse(0.0d) : 0.0d;
        return new PauseStatistics(totalPauseTime, percentPaused, averagePause, medianPause, p90Pause, p99Pause, maxPause);
    }
}
