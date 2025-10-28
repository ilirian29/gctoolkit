package com.example.app.core.aggregations;

import com.example.app.core.collections.XYDataSet;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.microsoft.gctoolkit.time.DateTimeStamp;

import java.util.EnumMap;
import java.util.Map;

public class HeapOccupancyAfterCollectionSummary extends HeapOccupancyAfterCollectionAggregation {

    private final EnumMap<GarbageCollectionTypes, XYDataSet> aggregations = new EnumMap<>(GarbageCollectionTypes.class);

    @Override
    public void addDataPoint(GarbageCollectionTypes gcType, DateTimeStamp timeStamp, long heapOccupancy) {
        aggregations.computeIfAbsent(gcType, key -> new XYDataSet()).add(timeStamp.getTimeStamp(), heapOccupancy);
    }

    public Map<GarbageCollectionTypes, XYDataSet> get() {
        return Map.copyOf(aggregations);
    }

    @Override
    public boolean hasWarning() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return aggregations.isEmpty();
    }

    @Override
    public String toString() {
        return "Collected " + aggregations.size() + " different collection types";
    }
}
