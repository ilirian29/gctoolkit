package com.microsoft.gctoolkit.io;

import java.time.Duration;

/**
 * Encapsulates a progress update while streaming a GC log.
 */
public final class ProgressUpdate {

    private final double fractionComplete;
    private final long processedBytes;
    private final long totalBytes;
    private final long elapsedMillis;
    private final long estimatedRemainingMillis;

    public ProgressUpdate(double fractionComplete,
                          long processedBytes,
                          long totalBytes,
                          long elapsedMillis,
                          long estimatedRemainingMillis) {
        this.fractionComplete = fractionComplete;
        this.processedBytes = processedBytes;
        this.totalBytes = totalBytes;
        this.elapsedMillis = elapsedMillis;
        this.estimatedRemainingMillis = estimatedRemainingMillis;
    }

    public double getFractionComplete() {
        return fractionComplete;
    }

    public long getProcessedBytes() {
        return processedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public long getEstimatedRemainingMillis() {
        return estimatedRemainingMillis;
    }

    /**
     * @return {@code true} if the total size of the log could not be determined.
     */
    public boolean isIndeterminate() {
        return totalBytes <= 0L;
    }

    /**
     * Convenience accessor to expose the elapsed time as a {@link Duration}.
     */
    public Duration elapsed() {
        return Duration.ofMillis(elapsedMillis);
    }

    /**
     * Convenience accessor to expose the ETA as a {@link Duration}. When the ETA is not known,
     * this returns {@link Duration#ZERO}.
     */
    public Duration eta() {
        return estimatedRemainingMillis < 0 ? Duration.ZERO : Duration.ofMillis(estimatedRemainingMillis);
    }
}
