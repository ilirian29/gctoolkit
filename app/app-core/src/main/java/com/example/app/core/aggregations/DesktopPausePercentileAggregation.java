package com.example.app.core.aggregations;

import com.microsoft.gctoolkit.aggregator.Aggregation;
import com.microsoft.gctoolkit.aggregator.Collates;

@Collates(DesktopPausePercentileAggregator.class)
public abstract class DesktopPausePercentileAggregation extends Aggregation {

    public abstract void recordPause(double duration);
}
