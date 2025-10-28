package com.yourorg.gcdesk.model;

import com.microsoft.gctoolkit.event.GCCause;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Aggregate view of GC causes including counts, average durations and breakdown by GC type.
 */
public class GCCauseSummary {

    private final Map<GCCause, Integer> causeCounts;
    private final Map<GCCause, Double> averageDurations;
    private final Map<GarbageCollectionTypes, Map<GCCause, Integer>> typeBreakdown;

    public GCCauseSummary(Map<GCCause, Integer> causeCounts, Map<GCCause, Double> averageDurations,
                          Map<GarbageCollectionTypes, Map<GCCause, Integer>> typeBreakdown) {
        this.causeCounts = copyCauseCounts(causeCounts);
        this.averageDurations = copyAverageDurations(averageDurations);
        this.typeBreakdown = copyTypeBreakdown(typeBreakdown);
    }

    public static GCCauseSummary empty() {
        return new GCCauseSummary(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    public Map<GCCause, Integer> getCauseCounts() {
        return causeCounts;
    }

    public Map<GCCause, Double> getAverageDurations() {
        return averageDurations;
    }

    public Map<GarbageCollectionTypes, Map<GCCause, Integer>> getTypeBreakdown() {
        return typeBreakdown;
    }

    private Map<GCCause, Integer> copyCauseCounts(Map<GCCause, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return Collections.emptyMap();
        }
        EnumMap<GCCause, Integer> copy = new EnumMap<>(GCCause.class);
        copy.putAll(counts);
        return Collections.unmodifiableMap(copy);
    }

    private Map<GCCause, Double> copyAverageDurations(Map<GCCause, Double> averages) {
        if (averages == null || averages.isEmpty()) {
            return Collections.emptyMap();
        }
        EnumMap<GCCause, Double> copy = new EnumMap<>(GCCause.class);
        copy.putAll(averages);
        return Collections.unmodifiableMap(copy);
    }

    private Map<GarbageCollectionTypes, Map<GCCause, Integer>> copyTypeBreakdown(
            Map<GarbageCollectionTypes, Map<GCCause, Integer>> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<GarbageCollectionTypes, Map<GCCause, Integer>> outer = new EnumMap<>(GarbageCollectionTypes.class);
        breakdown.forEach((type, counts) -> {
            if (counts == null || counts.isEmpty()) {
                outer.put(type, Collections.emptyMap());
            } else {
                EnumMap<GCCause, Integer> inner = new EnumMap<>(GCCause.class);
                inner.putAll(counts);
                outer.put(type, Collections.unmodifiableMap(inner));
            }
        });
        return Collections.unmodifiableMap(outer);
    }
}
