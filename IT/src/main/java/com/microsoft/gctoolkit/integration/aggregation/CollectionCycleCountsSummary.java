package com.microsoft.gctoolkit.integration.aggregation;

import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

import java.io.PrintStream;
import java.util.EnumMap;
import java.util.concurrent.atomic.LongAdder;

public class CollectionCycleCountsSummary extends CollectionCycleCountsAggregation {

    private final EnumMap<GarbageCollectionTypes, LongAdder> collectionCycleCounts = new EnumMap<>(GarbageCollectionTypes.class);
    @Override
    public void count(GarbageCollectionTypes gcType) {
        collectionCycleCounts.computeIfAbsent(gcType, key -> new LongAdder()).increment();
    }

    private String format = "%s : %s\n";
    public void printOn(PrintStream printStream) {
        collectionCycleCounts.forEach((k, v) -> printStream.printf(format, k, v.intValue()));
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
