package com.example.app.core.aggregations;

import com.microsoft.gctoolkit.aggregator.Aggregates;
import com.microsoft.gctoolkit.aggregator.Aggregator;
import com.microsoft.gctoolkit.aggregator.EventSource;
import com.microsoft.gctoolkit.event.g1gc.G1GCConcurrentEvent;
import com.microsoft.gctoolkit.event.g1gc.G1GCPauseEvent;
import com.microsoft.gctoolkit.event.generational.GenerationalGCPauseEvent;
import com.microsoft.gctoolkit.event.shenandoah.ShenandoahCycle;
import com.microsoft.gctoolkit.event.zgc.ZGCFullCollection;
import com.microsoft.gctoolkit.event.zgc.ZGCOldCollection;
import com.microsoft.gctoolkit.event.zgc.ZGCYoungCollection;

@Aggregates({EventSource.G1GC, EventSource.GENERATIONAL, EventSource.ZGC, EventSource.SHENANDOAH})
public class DesktopGCCauseFrequencyAggregator extends Aggregator<DesktopGCCauseFrequencyAggregation> {

    public DesktopGCCauseFrequencyAggregator(DesktopGCCauseFrequencyAggregation aggregation) {
        super(aggregation);
        register(G1GCPauseEvent.class, this::recordCause);
        register(G1GCConcurrentEvent.class, this::recordCause);
        register(GenerationalGCPauseEvent.class, this::recordCause);
        register(ZGCFullCollection.class, this::recordCause);
        register(ZGCOldCollection.class, this::recordCause);
        register(ZGCYoungCollection.class, this::recordCause);
        register(ShenandoahCycle.class, this::recordCause);
    }

    private void recordCause(G1GCPauseEvent event) {
        aggregation().recordCause(event.getGarbageCollectionType(), event.getGCCause(), event.getDuration());
    }

    private void recordCause(G1GCConcurrentEvent event) {
        aggregation().recordCause(event.getGarbageCollectionType(), event.getGCCause(), event.getDuration());
    }

    private void recordCause(GenerationalGCPauseEvent event) {
        aggregation().recordCause(event.getGarbageCollectionType(), event.getGCCause(), event.getDuration());
    }

    private void recordCause(ZGCFullCollection event) {
        aggregation().recordCause(event.getGarbageCollectionType(), event.getGCCause(), event.getDuration());
    }

    private void recordCause(ZGCOldCollection event) {
        aggregation().recordCause(event.getGarbageCollectionType(), event.getGCCause(), event.getDuration());
    }

    private void recordCause(ZGCYoungCollection event) {
        aggregation().recordCause(event.getGarbageCollectionType(), event.getGCCause(), event.getDuration());
    }

    private void recordCause(ShenandoahCycle event) {
        aggregation().recordCause(event.getGarbageCollectionType(), event.getGCCause(), event.getDuration());
    }
}
