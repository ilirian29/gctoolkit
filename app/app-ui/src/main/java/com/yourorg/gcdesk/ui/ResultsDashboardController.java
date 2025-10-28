package com.yourorg.gcdesk.ui;

import com.example.app.core.reporting.ReportFormat;
import com.example.app.core.reporting.ReportGenerationException;
import com.example.app.core.reporting.ReportService;
import com.microsoft.gctoolkit.event.GCCause;
import com.microsoft.gctoolkit.event.GarbageCollectionTypes;
import com.yourorg.gcdesk.model.AnalysisResult;
import com.yourorg.gcdesk.model.CollectionCycleSummary;
import com.yourorg.gcdesk.model.GCCauseSummary;
import com.yourorg.gcdesk.model.HeapOccupancySummary;
import com.yourorg.gcdesk.model.PauseStatistics;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Controller responsible for presenting the analysed results, including charts, tables and summary
 * statistics with contextual warning badges.
 */
public class ResultsDashboardController {

    @FXML
    private Label sourceLabel;

    @FXML
    private Label analyzedAtLabel;

    @FXML
    private Label timelineWarningBadge;

    @FXML
    private Label summaryWarningBadge;

    @FXML
    private ComboBox<ReportFormat> exportFormatChoice;

    @FXML
    private Button exportButton;

    @FXML
    private Label exportStatusLabel;

    @FXML
    private LineChart<Number, Number> heapOccupancyChart;

    @FXML
    private TableView<CauseRow> causeTable;

    @FXML
    private TableColumn<CauseRow, String> causeColumn;

    @FXML
    private TableColumn<CauseRow, Number> causeCountColumn;

    @FXML
    private TableColumn<CauseRow, Number> causeAverageDurationColumn;

    @FXML
    private TableView<CycleRow> cycleTable;

    @FXML
    private TableColumn<CycleRow, String> cycleTypeColumn;

    @FXML
    private TableColumn<CycleRow, Number> cycleCountColumn;

    @FXML
    private PieChart causeDistributionChart;

    @FXML
    private Label totalPauseLabel;

    @FXML
    private Label percentPausedLabel;

    @FXML
    private Label averagePauseLabel;

    @FXML
    private Label medianPauseLabel;

    @FXML
    private Label p90PauseLabel;

    @FXML
    private Label p99PauseLabel;

    @FXML
    private Label maxPauseLabel;

    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.getDefault());
    private ReportService reportService = new ReportService();
    private AnalysisResult currentResult;

    @FXML
    private void initialize() {
        hideTimelineWarning();
        hideSummaryWarning();

        causeTable.setPlaceholder(new Label("No GC cause data available."));
        cycleTable.setPlaceholder(new Label("No collection cycle data available."));

        causeColumn.setCellValueFactory(new PropertyValueFactory<>("cause"));
        causeCountColumn.setCellValueFactory(new PropertyValueFactory<>("count"));
        causeAverageDurationColumn.setCellValueFactory(new PropertyValueFactory<>("averageDuration"));

        cycleTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        cycleCountColumn.setCellValueFactory(new PropertyValueFactory<>("count"));

        heapOccupancyChart.setAnimated(false);
        heapOccupancyChart.setLegendVisible(true);
        heapOccupancyChart.setCreateSymbols(false);

        if (exportFormatChoice != null) {
            exportFormatChoice.setItems(FXCollections.observableArrayList(ReportFormat.values()));
            exportFormatChoice.getSelectionModel().select(ReportFormat.PDF);
            exportFormatChoice.valueProperty().addListener((obs, oldValue, newValue) -> updateExportControls());
        }
        if (exportButton != null) {
            exportButton.setDisable(true);
        }
        if (exportStatusLabel != null) {
            exportStatusLabel.setText("Generate a report once analysis results are available.");
        }
    }

    /**
     * Populate the dashboard with the supplied result.
     *
     * @param result computed analysis result
     */
    public void displayResult(AnalysisResult result) {
        Objects.requireNonNull(result, "result");

        hideTimelineWarning();
        hideSummaryWarning();

        Path source = result.getSource();
        sourceLabel.setText(source.toString());
        analyzedAtLabel.setText(timestampFormatter.format(result.getAnalyzedAt().atZone(ZoneId.systemDefault())));

        populateHeapOccupancy(result.getHeapOccupancySummary());
        populateCauseTables(result.getGcCauseSummary());
        populateCycleTable(result.getCollectionCycleSummary());
        populateSummary(result.getPauseStatistics());

        currentResult = result;
        if (exportStatusLabel != null) {
            exportStatusLabel.setText("Ready to export a report.");
        }
        updateExportControls();
    }

    @FXML
    private void onExportReport() {
        if (currentResult == null) {
            updateExportStatus("Run an analysis before exporting a report.");
            return;
        }
        if (reportService == null) {
            updateExportStatus("Report service is not configured.");
            return;
        }
        ReportFormat format = exportFormatChoice != null ? exportFormatChoice.getValue() : ReportFormat.PDF;
        if (format == null) {
            updateExportStatus("Select a report format before exporting.");
            return;
        }
        Window window = exportButton != null && exportButton.getScene() != null
                ? exportButton.getScene().getWindow()
                : null;
        if (window == null) {
            updateExportStatus("Unable to open directory chooser.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select export directory");
        java.io.File directory = chooser.showDialog(window);
        if (directory == null) {
            updateExportStatus("Export cancelled.");
            return;
        }

        try {
            Path reportPath = reportService.generateReport(currentResult, directory.toPath(), format);
            updateExportStatus("Report saved to " + reportPath.getFileName());
        } catch (ReportGenerationException ex) {
            updateExportStatus("Export failed: " + ex.getMessage());
        }
    }

    private void populateHeapOccupancy(HeapOccupancySummary summary) {
        heapOccupancyChart.getData().clear();
        Map<GarbageCollectionTypes, List<HeapOccupancySummary.XYPoint>> seriesByType = summary.getSeriesByType();
        if (seriesByType == null || seriesByType.isEmpty()) {
            showTimelineWarning("No heap occupancy data available.");
            return;
        }

        hideTimelineWarning();
        seriesByType.forEach((type, points) -> {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName(type.getLabel());
            points.forEach(point -> series.getData().add(new XYChart.Data<>(point.getX(), point.getY())));
            heapOccupancyChart.getData().add(series);
        });
    }

    private void populateCauseTables(GCCauseSummary summary) {
        Map<GCCause, Integer> causeCounts = summary.getCauseCounts();
        Map<GCCause, Double> averageDurations = summary.getAverageDurations();

        ObservableList<CauseRow> causeRows = causeCounts.entrySet().stream()
                .map(entry -> new CauseRow(entry.getKey().getLabel(), entry.getValue(),
                        averageDurations.getOrDefault(entry.getKey(), 0.0d)))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        causeTable.setItems(causeRows);

        Map<GCCause, Integer> totals = summary.getCauseCounts();
        ObservableList<PieChart.Data> pieData = totals.entrySet().stream()
                .map(entry -> new PieChart.Data(entry.getKey().getLabel(), entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        causeDistributionChart.setData(pieData);

        if (totals.isEmpty()) {
            showSummaryWarning("No GC cause events detected.");
        } else if (summaryWarningBadge.isVisible()
                && "No GC cause events detected.".equals(summaryWarningBadge.getText())) {
            hideSummaryWarning();
        }
    }

    private void populateCycleTable(CollectionCycleSummary summary) {
        ObservableList<CycleRow> cycleRows = summary.getCounts().entrySet().stream()
                .map(entry -> new CycleRow(entry.getKey().getLabel(), entry.getValue()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        cycleTable.setItems(cycleRows);
    }

    private void populateSummary(PauseStatistics statistics) {
        totalPauseLabel.setText(formatMillis(statistics.getTotalPauseTime()));
        percentPausedLabel.setText(String.format(Locale.getDefault(), "%.2f %%", statistics.getPercentPaused()));
        averagePauseLabel.setText(formatMillis(statistics.getAveragePause()));
        medianPauseLabel.setText(formatMillis(statistics.getMedianPause()));
        p90PauseLabel.setText(formatMillis(statistics.getP90Pause()));
        p99PauseLabel.setText(formatMillis(statistics.getP99Pause()));
        maxPauseLabel.setText(formatMillis(statistics.getMaxPause()));

        if (statistics.getPercentPaused() > 5.0d || statistics.getMaxPause() > 1000.0d) {
            showSummaryWarning("High pause impact detected.");
        } else if (!causeDistributionChart.getData().isEmpty()) {
            hideSummaryWarning();
        }
    }

    private String formatMillis(double value) {
        return String.format(Locale.getDefault(), "%.2f ms", value);
    }

    private void showTimelineWarning(String message) {
        timelineWarningBadge.setText(message);
        timelineWarningBadge.setManaged(true);
        timelineWarningBadge.setVisible(true);
    }

    private void hideTimelineWarning() {
        timelineWarningBadge.setManaged(false);
        timelineWarningBadge.setVisible(false);
    }

    private void showSummaryWarning(String message) {
        summaryWarningBadge.setText(message);
        summaryWarningBadge.setManaged(true);
        summaryWarningBadge.setVisible(true);
    }

    private void hideSummaryWarning() {
        summaryWarningBadge.setManaged(false);
        summaryWarningBadge.setVisible(false);
    }

    public void setReportService(ReportService reportService) {
        this.reportService = Objects.requireNonNull(reportService, "reportService");
        updateExportControls();
    }

    private void updateExportControls() {
        if (exportButton != null) {
            boolean disable = currentResult == null;
            if (exportFormatChoice != null) {
                disable = disable || exportFormatChoice.getSelectionModel().isEmpty();
            }
            exportButton.setDisable(disable);
        }
    }

    private void updateExportStatus(String message) {
        if (exportStatusLabel != null) {
            exportStatusLabel.setText(message);
        }
    }

    /**
     * Row model for the GC cause table.
     */
    public static class CauseRow {
        private final StringProperty cause = new SimpleStringProperty();
        private final IntegerProperty count = new SimpleIntegerProperty();
        private final DoubleProperty averageDuration = new SimpleDoubleProperty();

        public CauseRow(String cause, int count, double averageDuration) {
            this.cause.set(cause);
            this.count.set(count);
            this.averageDuration.set(averageDuration);
        }

        public String getCause() {
            return cause.get();
        }

        public StringProperty causeProperty() {
            return cause;
        }

        public int getCount() {
            return count.get();
        }

        public IntegerProperty countProperty() {
            return count;
        }

        public double getAverageDuration() {
            return averageDuration.get();
        }

        public DoubleProperty averageDurationProperty() {
            return averageDuration;
        }
    }

    /**
     * Row model for the collection cycle table.
     */
    public static class CycleRow {
        private final StringProperty type = new SimpleStringProperty();
        private final IntegerProperty count = new SimpleIntegerProperty();

        public CycleRow(String type, int count) {
            this.type.set(type);
            this.count.set(count);
        }

        public String getType() {
            return type.get();
        }

        public StringProperty typeProperty() {
            return type;
        }

        public int getCount() {
            return count.get();
        }

        public IntegerProperty countProperty() {
            return count;
        }
    }
}
