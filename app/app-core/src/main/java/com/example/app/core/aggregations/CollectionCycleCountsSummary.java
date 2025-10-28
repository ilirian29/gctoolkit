package com.example.app.core.aggregations;

import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CollectionCycleCountsSummary extends CollectionCycleCountsAggregation {

    private final Map<GarbageCollectionTypes, AtomicInteger> collectionCycleCounts = new ConcurrentHashMap<>();

    @Override
    public void count(GarbageCollectionTypes gcType) {
        collectionCycleCounts.computeIfAbsent(gcType, key -> new AtomicInteger()).incrementAndGet();
    }

    public Map<GarbageCollectionTypes, Integer> getCounts() {
        return collectionCycleCounts.entrySet().stream().collect(
                java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    public void printOn(PrintStream printStream) {
        collectionCycleCounts.forEach((k, v) -> printStream.printf("%s : %s%n", k, v));
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
