package com.example.app.core.reporting;

import com.microsoft.gctoolkit.event.GCCause;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.yourorg.gcdesk.model.GCCauseSummary;
import com.yourorg.gcdesk.model.HeapOccupancySummary;
import com.yourorg.gcdesk.model.HeapOccupancySummary.XYPoint;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.Styler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Helper responsible for turning aggregated data into embeddable chart images.
 */
class ReportChartGenerator {

    private static final int CHART_WIDTH = 900;
    private static final int CHART_HEIGHT = 480;

    Optional<String> renderHeapOccupancyChart(HeapOccupancySummary summary) throws ReportGenerationException {
        Map<GarbageCollectionTypes, List<XYPoint>> seriesByType = summary.getSeriesByType();
        if (seriesByType == null || seriesByType.isEmpty()) {
            return Optional.empty();
        }

        XYChart chart = new XYChartBuilder()
                .width(CHART_WIDTH)
                .height(CHART_HEIGHT)
                .title("Heap occupancy over time")
                .xAxisTitle("Time (s)")
                .yAxisTitle("Heap occupancy (MB)")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setPlotMargin(10);
        chart.getStyler().setPlotContentSize(0.85);
        chart.getStyler().setMarkerSize(4);

        for (Map.Entry<GarbageCollectionTypes, List<XYPoint>> entry : seriesByType.entrySet()) {
            List<Double> xValues = new ArrayList<>();
            List<Double> yValues = new ArrayList<>();
            for (XYPoint point : entry.getValue()) {
                xValues.add(point.getX());
                yValues.add(point.getY());
            }
            if (!xValues.isEmpty()) {
                chart.addSeries(entry.getKey().getLabel(), xValues, yValues);
            }
        }

        if (chart.getSeriesMap().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toDataUri(chart));
    }

    Optional<String> renderCauseDistributionChart(GCCauseSummary summary) throws ReportGenerationException {
        Map<GCCause, Integer> counts = summary.getCauseCounts();
        if (counts == null || counts.isEmpty()) {
            return Optional.empty();
        }

        PieChart chart = new PieChartBuilder()
                .width(CHART_WIDTH)
                .height(CHART_HEIGHT)
                .title("GC cause distribution")
                .build();

        PieStyler styler = chart.getStyler();
        styler.setLegendVisible(true);
        styler.setLegendPosition(Styler.LegendPosition.OutsideE);
        styler.setLabelsVisible(true);
        styler.setLabelsDistance(1.15);
        styler.setForceAllLabelsVisible(true);
        styler.setPlotContentSize(0.9);

        counts.entrySet().stream()
                .sorted(Map.Entry.<GCCause, Integer>comparingByValue().reversed())
                .collect(Collectors.toList())
                .forEach(entry -> chart.addSeries(entry.getKey().getLabel(), entry.getValue()));

        if (chart.getSeriesMap().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toDataUri(chart));
    }

    private String toDataUri(XYChart chart) throws ReportGenerationException {
        try {
            byte[] bytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
            return encode(bytes);
        } catch (IOException e) {
            throw new ReportGenerationException("Unable to render XY chart", e);
        }
    }

    private String toDataUri(PieChart chart) throws ReportGenerationException {
        try {
            byte[] bytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
            return encode(bytes);
        } catch (IOException e) {
            throw new ReportGenerationException("Unable to render pie chart", e);
        }
    }

    private String encode(byte[] bytes) {
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return "data:image/png;base64," + base64;
    }
}
