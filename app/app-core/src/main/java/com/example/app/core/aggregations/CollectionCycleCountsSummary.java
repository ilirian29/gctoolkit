package com.example.app.core.aggregations;

import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

import java.io.PrintStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

public class CollectionCycleCountsSummary extends CollectionCycleCountsAggregation {

    private final EnumMap<GarbageCollectionTypes, LongAdder> collectionCycleCounts = new EnumMap<>(GarbageCollectionTypes.class);

    @Override
    public void count(GarbageCollectionTypes gcType) {
        collectionCycleCounts.computeIfAbsent(gcType, key -> new LongAdder()).increment();
    }

    public Map<GarbageCollectionTypes, Integer> getCounts() {
        return collectionCycleCounts.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().intValue()));
    }

    public void printOn(PrintStream printStream) {
        collectionCycleCounts.forEach((k, v) -> printStream.printf("%s : %s%n", k, v.intValue()));
    }

    @Override
    public boolean hasWarning() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return collectionCycleCounts.isEmpty();
    }
}
