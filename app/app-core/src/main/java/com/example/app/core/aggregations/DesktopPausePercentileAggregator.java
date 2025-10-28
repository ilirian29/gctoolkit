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
public class DesktopPausePercentileAggregator extends Aggregator<DesktopPausePercentileAggregation> {

    public DesktopPausePercentileAggregator(DesktopPausePercentileAggregation aggregation) {
        super(aggregation);
        register(G1GCPauseEvent.class, this::record);
        register(G1GCConcurrentEvent.class, this::record);
        register(GenerationalGCPauseEvent.class, this::record);
        register(ZGCFullCollection.class, this::record);
        register(ZGCOldCollection.class, this::record);
        register(ZGCYoungCollection.class, this::record);
        register(ShenandoahCycle.class, this::record);
    }

    private void record(G1GCPauseEvent event) {
        aggregation().recordPause(event.getDuration());
    }

    private void record(G1GCConcurrentEvent event) {
        aggregation().recordPause(event.getDuration());
    }

    private void record(GenerationalGCPauseEvent event) {
        aggregation().recordPause(event.getDuration());
    }

    private void record(ZGCFullCollection event) {
        aggregation().recordPause(event.getDuration());
    }

    private void record(ZGCOldCollection event) {
        aggregation().recordPause(event.getDuration());
    }

    private void record(ZGCYoungCollection event) {
        aggregation().recordPause(event.getDuration());
    }

    private void record(ShenandoahCycle event) {
        aggregation().recordPause(event.getDuration());
    }
}
