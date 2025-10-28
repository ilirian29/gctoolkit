package com.example.app.core.aggregations;

import com.microsoft.gctoolkit.aggregator.Aggregation;
import com.microsoft.gctoolkit.aggregator.Collates;
import com.microsoft.gctoolkit.event.GCCause;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;

@Collates(DesktopGCCauseFrequencyAggregator.class)
public abstract class DesktopGCCauseFrequencyAggregation extends Aggregation {

    public abstract void recordCause(GarbageCollectionTypes type, GCCause cause, double duration);
}
