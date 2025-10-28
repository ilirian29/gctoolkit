package com.example.app.core.aggregations;

import com.microsoft.gctoolkit.aggregator.Aggregation;
import com.microsoft.gctoolkit.aggregator.Collates;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

@Collates(CollectionCycleCountsAggregator.class)
public abstract class CollectionCycleCountsAggregation extends Aggregation {

    public abstract void count(GarbageCollectionTypes gcType);
}
