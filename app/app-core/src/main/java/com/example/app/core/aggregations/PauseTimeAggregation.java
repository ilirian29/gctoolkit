package com.example.app.core.aggregations;

import com.microsoft.gctoolkit.aggregator.Aggregation;
import com.microsoft.gctoolkit.aggregator.Collates;

@Collates(PauseTimeAggregator.class)
public abstract class PauseTimeAggregation extends Aggregation {

    public abstract void recordPauseDuration(double duration);
}
