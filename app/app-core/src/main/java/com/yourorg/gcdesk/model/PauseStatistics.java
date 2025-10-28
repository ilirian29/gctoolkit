package com.yourorg.gcdesk.model;

/**
 * Aggregated pause duration statistics derived from GC logs.
 */
public class PauseStatistics {

    private final double totalPauseTime;
    private final double percentPaused;
    private final double averagePause;
    private final double medianPause;
    private final double p90Pause;
    private final double p99Pause;
    private final double maxPause;

    public PauseStatistics(double totalPauseTime, double percentPaused, double averagePause,
                           double medianPause, double p90Pause, double p99Pause, double maxPause) {
        this.totalPauseTime = totalPauseTime;
        this.percentPaused = percentPaused;
        this.averagePause = averagePause;
        this.medianPause = medianPause;
        this.p90Pause = p90Pause;
        this.p99Pause = p99Pause;
        this.maxPause = maxPause;
    }

    public static PauseStatistics empty() {
        return new PauseStatistics(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
    }

    public double getTotalPauseTime() {
        return totalPauseTime;
    }

    public double getPercentPaused() {
        return percentPaused;
    }

    public double getAveragePause() {
        return averagePause;
    }

    public double getMedianPause() {
        return medianPause;
    }

    public double getP90Pause() {
        return p90Pause;
    }

    public double getP99Pause() {
        return p99Pause;
    }

    public double getMaxPause() {
        return maxPause;
    }
}
