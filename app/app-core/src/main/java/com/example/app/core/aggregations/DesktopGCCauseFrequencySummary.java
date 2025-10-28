package com.example.app.core.aggregations;

import com.microsoft.gctoolkit.event.GCCause;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;

public class DesktopGCCauseFrequencySummary extends DesktopGCCauseFrequencyAggregation {

    private final Map<GCCause, AtomicInteger> causeCounts = new ConcurrentHashMap<>();
    private final Map<GCCause, DoubleAdder> causeDurations = new ConcurrentHashMap<>();
    private final Map<GarbageCollectionTypes, EnumMap<GCCause, AtomicInteger>> typeBreakdown = new ConcurrentHashMap<>();

    @Override
    public void recordCause(GarbageCollectionTypes type, GCCause cause, double duration) {
        GCCause safeCause = cause == null ? GCCause.UNKNOWN_GCCAUSE : cause;
        GarbageCollectionTypes safeType = type == null ? GarbageCollectionTypes.Unknown : type;
        causeCounts.computeIfAbsent(safeCause, key -> new AtomicInteger()).incrementAndGet();
        causeDurations.computeIfAbsent(safeCause, key -> new DoubleAdder()).add(duration);
        typeBreakdown.computeIfAbsent(safeType, key -> new EnumMap<>(GCCause.class))
                .computeIfAbsent(safeCause, key -> new AtomicInteger())
                .incrementAndGet();
    }

    public Map<GCCause, Integer> getCauseCounts() {
        return causeCounts.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    public Map<GCCause, Double> getAverageDurationsByCause() {
        return causeDurations.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> entry.getValue().sum() / Math.max(1, causeCounts.get(entry.getKey()).get())));
    }

    public Map<GarbageCollectionTypes, Map<GCCause, Integer>> getTypeBreakdown() {
        return typeBreakdown.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream().collect(
                                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> e.getValue().get()))));
    }

    @Override
    public boolean hasWarning() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return causeCounts.isEmpty();
    }
}
