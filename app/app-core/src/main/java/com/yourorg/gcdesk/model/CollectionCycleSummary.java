package com.yourorg.gcdesk.model;

import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Snapshot of collection cycle counts grouped by GC type.
 */
public class CollectionCycleSummary {

    private final Map<GarbageCollectionTypes, Integer> counts;

    public CollectionCycleSummary(Map<GarbageCollectionTypes, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            this.counts = Collections.emptyMap();
        } else {
            EnumMap<GarbageCollectionTypes, Integer> copy = new EnumMap<>(GarbageCollectionTypes.class);
            copy.putAll(counts);
            this.counts = Collections.unmodifiableMap(copy);
        }
    }

    public static CollectionCycleSummary empty() {
        return new CollectionCycleSummary(Collections.emptyMap());
    }

    public Map<GarbageCollectionTypes, Integer> getCounts() {
        return counts;
    }
}
