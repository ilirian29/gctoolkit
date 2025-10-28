package com.example.app.core.aggregations;

public class PauseTimeSummary extends PauseTimeAggregation {

    private double totalPauseTime;

    @Override
    public boolean hasWarning() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void recordPauseDuration(double duration) {
        totalPauseTime += duration;
    }

    public double getTotalPauseTime() {
        return totalPauseTime;
    }

    public double getPercentPaused() {
        return (totalPauseTime / super.estimatedRuntime()) * 100.0d;
    }
}
