package com.example.app.core.reporting;

import java.util.List;
import java.util.Objects;

/**
 * Immutable data structure consumed by the reporting templates.
 */
public class ReportViewModel {

    private final String title;
    private final String sourcePath;
    private final String generatedAt;
    private final SummaryMetrics summary;
    private final List<ChartViewModel> charts;
    private final List<RecommendationViewModel> recommendations;
    private final List<CauseRowViewModel> causeRows;
    private final List<CycleRowViewModel> cycleRows;
    private final String runtimeSummary;

    public ReportViewModel(String title, String sourcePath, String generatedAt, SummaryMetrics summary,
                           List<ChartViewModel> charts, List<RecommendationViewModel> recommendations,
                           List<CauseRowViewModel> causeRows, List<CycleRowViewModel> cycleRows,
                           String runtimeSummary) {
        this.title = Objects.requireNonNull(title, "title");
        this.sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        this.summary = Objects.requireNonNull(summary, "summary");
        this.charts = List.copyOf(charts);
        this.recommendations = List.copyOf(recommendations);
        this.causeRows = List.copyOf(causeRows);
        this.cycleRows = List.copyOf(cycleRows);
        this.runtimeSummary = Objects.requireNonNull(runtimeSummary, "runtimeSummary");
    }

    public String getTitle() {
        return title;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public SummaryMetrics getSummary() {
        return summary;
    }

    public List<ChartViewModel> getCharts() {
        return charts;
    }

    public boolean hasCharts() {
        return !charts.isEmpty();
    }

    public List<RecommendationViewModel> getRecommendations() {
        return recommendations;
    }

    public boolean hasRecommendations() {
        return !recommendations.isEmpty();
    }

    public List<CauseRowViewModel> getCauseRows() {
        return causeRows;
    }

    public boolean hasCauseRows() {
        return !causeRows.isEmpty();
    }

    public List<CycleRowViewModel> getCycleRows() {
        return cycleRows;
    }

    public boolean hasCycleRows() {
        return !cycleRows.isEmpty();
    }

    public String getRuntimeSummary() {
        return runtimeSummary;
    }

    /**
     * Container for the key metric values displayed at the top of the report.
     */
    public static class SummaryMetrics {
        private final double totalPauseTime;
        private final double percentPaused;
        private final double averagePause;
        private final double medianPause;
        private final double p90Pause;
        private final double p99Pause;
        private final double maxPause;

        public SummaryMetrics(double totalPauseTime, double percentPaused, double averagePause,
                               double medianPause, double p90Pause, double p99Pause, double maxPause) {
            this.totalPauseTime = totalPauseTime;
            this.percentPaused = percentPaused;
            this.averagePause = averagePause;
            this.medianPause = medianPause;
            this.p90Pause = p90Pause;
            this.p99Pause = p99Pause;
            this.maxPause = maxPause;
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

    /**
     * Representation of a chart rendered to an inline data URI.
     */
    public static class ChartViewModel {
        private final String title;
        private final String description;
        private final String imageDataUri;

        public ChartViewModel(String title, String description, String imageDataUri) {
            this.title = Objects.requireNonNull(title, "title");
            this.description = Objects.requireNonNull(description, "description");
            this.imageDataUri = Objects.requireNonNull(imageDataUri, "imageDataUri");
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getImageDataUri() {
            return imageDataUri;
        }
    }

    /**
     * Simple recommendation text block.
     */
    public static class RecommendationViewModel {
        private final String title;
        private final String detail;

        public RecommendationViewModel(String title, String detail) {
            this.title = Objects.requireNonNull(title, "title");
            this.detail = Objects.requireNonNull(detail, "detail");
        }

        public String getTitle() {
            return title;
        }

        public String getDetail() {
            return detail;
        }
    }

    /**
     * Row describing a GC cause aggregate.
     */
    public static class CauseRowViewModel {
        private final String cause;
        private final int count;
        private final double averageDuration;

        public CauseRowViewModel(String cause, int count, double averageDuration) {
            this.cause = Objects.requireNonNull(cause, "cause");
            this.count = count;
            this.averageDuration = averageDuration;
        }

        public String getCause() {
            return cause;
        }

        public int getCount() {
            return count;
        }

        public double getAverageDuration() {
            return averageDuration;
        }
    }

    /**
     * Row describing a GC type cycle count.
     */
    public static class CycleRowViewModel {
        private final String type;
        private final int count;

        public CycleRowViewModel(String type, int count) {
            this.type = Objects.requireNonNull(type, "type");
            this.count = count;
        }

        public String getType() {
            return type;
        }

        public int getCount() {
            return count;
        }
    }
}
