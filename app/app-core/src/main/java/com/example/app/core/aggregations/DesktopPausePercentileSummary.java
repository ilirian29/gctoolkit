package com.example.app.core.aggregations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

public class DesktopPausePercentileSummary extends DesktopPausePercentileAggregation {

    private final List<Double> durations = new ArrayList<>();

    @Override
    public void recordPause(double duration) {
        durations.add(duration);
    }

    public double getMedianPause() {
        return getPercentile(50.0);
    }

    public double getP90Pause() {
        return getPercentile(90.0);
    }

    public double getP99Pause() {
        return getPercentile(99.0);
    }

    public double getAveragePause() {
        return durations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0d);
    }

    public double getPercentile(double percentile) {
        if (durations.isEmpty()) {
            return 0.0d;
        }
        List<Double> sorted = new ArrayList<>(durations);
        Collections.sort(sorted);
        double rank = percentile / 100.0d * (sorted.size() - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        double lower = sorted.get(lowerIndex);
        double upper = sorted.get(upperIndex);
        if (lowerIndex == upperIndex) {
            return lower;
        }
        double fraction = rank - lowerIndex;
        return lower + (upper - lower) * fraction;
    }

    public OptionalDouble getMaxPause() {
        return durations.stream().mapToDouble(Double::doubleValue).max();
    }

    @Override
    public boolean hasWarning() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return durations.isEmpty();
    }
}
