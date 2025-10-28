package com.yourorg.gcdesk.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model containing the results of analysing a GC log.
 */
public class AnalysisResult {

    private final UUID id;
    private final Path source;
    private final Instant analyzedAt;
    private final HeapOccupancySummary heapOccupancySummary;
    private final PauseStatistics pauseStatistics;
    private final CollectionCycleSummary collectionCycleSummary;
    private final GCCauseSummary gcCauseSummary;

    public AnalysisResult(UUID id, Path source, Instant analyzedAt, HeapOccupancySummary heapOccupancySummary,
                          PauseStatistics pauseStatistics, CollectionCycleSummary collectionCycleSummary,
                          GCCauseSummary gcCauseSummary) {
        this.id = Objects.requireNonNull(id, "id");
        this.source = Objects.requireNonNull(source, "source");
        this.analyzedAt = Objects.requireNonNull(analyzedAt, "analyzedAt");
        this.heapOccupancySummary = Objects.requireNonNull(heapOccupancySummary, "heapOccupancySummary");
        this.pauseStatistics = Objects.requireNonNull(pauseStatistics, "pauseStatistics");
        this.collectionCycleSummary = Objects.requireNonNull(collectionCycleSummary, "collectionCycleSummary");
        this.gcCauseSummary = Objects.requireNonNull(gcCauseSummary, "gcCauseSummary");
    }

    public UUID getId() {
        return id;
    }

    public Path getSource() {
        return source;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public HeapOccupancySummary getHeapOccupancySummary() {
        return heapOccupancySummary;
    }

    public PauseStatistics getPauseStatistics() {
        return pauseStatistics;
    }

    public CollectionCycleSummary getCollectionCycleSummary() {
        return collectionCycleSummary;
    }

    public GCCauseSummary getGcCauseSummary() {
        return gcCauseSummary;
    }
}
