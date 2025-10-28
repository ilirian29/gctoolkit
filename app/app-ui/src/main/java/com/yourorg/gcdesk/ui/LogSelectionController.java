package com.yourorg.gcdesk.ui;

import com.example.app.core.logging.Logging;
import com.yourorg.gcdesk.AnalysisException;
import com.yourorg.gcdesk.AnalysisService;
import com.yourorg.gcdesk.CrashReportService;
import com.yourorg.gcdesk.model.AnalysisResult;
import com.yourorg.gcdesk.plugins.PluginDescriptor;
import com.yourorg.gcdesk.plugins.PluginRegistry;
import com.yourorg.gcdesk.plugins.PluginStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Controller backing the log selection screen. It validates user input, surfaces warnings and
 * orchestrates execution of the {@link AnalysisService} while delegating progress reporting and
 * result rendering to collaborators.
 */
public class LogSelectionController {

    private static final org.slf4j.Logger LOGGER = Logging.getLogger(LogSelectionController.class);

    @FXML
    private TextField logPathField;

    @FXML
    private Button analyzeButton;

    @FXML
    private Button browseButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Label warningBadge;

    @FXML
    private Label pluginSummaryLabel;

    @FXML
    private ListView<PluginDescriptor> pluginListView;

    private AnalysisService analysisService = new AnalysisService();
    private CrashReportService crashReportService = new CrashReportService();
    private AnalysisProgressController progressController;
    private Stage progressDialogStage;
    private Consumer<AnalysisResult> onAnalysisCompleted;
    private Consumer<Throwable> onAnalysisFailed;
    private ExecutorService analysisExecutor;
    private PluginRegistry pluginRegistry = analysisService.getPluginRegistry();

    @FXML
    private void initialize() {
        warningBadge.setManaged(false);
        warningBadge.setVisible(false);
        statusLabel.setText("Select a GC log to begin.");
        analyzeButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> logPathField.getText() == null || logPathField.getText().trim().isEmpty(),
                logPathField.textProperty()));

        if (pluginListView != null) {
            pluginListView.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(PluginDescriptor descriptor, boolean empty) {
                    super.updateItem(descriptor, empty);
                    if (empty || descriptor == null) {
                        setText(null);
                        setTooltip(null);
                        return;
                    }
                    String statusText = switch (descriptor.status()) {
                        case LOADED -> "Loaded";
                        case INCOMPATIBLE -> "Incompatible";
                        case FAILED -> "Failed";
                    };
                    StringBuilder text = new StringBuilder();
                    text.append(descriptor.name() != null ? descriptor.name() : descriptor.id());
                    if (descriptor.version() != null && !descriptor.version().isBlank() && !"-".equals(descriptor.version())) {
                        text.append(' ').append(descriptor.version());
                    }
                    text.append(" – ").append(statusText);
                    String detail = !descriptor.errors().isEmpty() ? descriptor.errors().get(0)
                            : (!descriptor.warnings().isEmpty() ? descriptor.warnings().get(0) : "");
                    if (!detail.isBlank()) {
                        text.append(": ").append(detail);
                    }
                    setText(text.toString());

                    StringBuilder tooltip = new StringBuilder();
                    if (descriptor.description() != null && !descriptor.description().isBlank()) {
                        tooltip.append(descriptor.description());
                    }
                    if (!descriptor.providedAggregations().isEmpty()) {
                        if (tooltip.length() > 0) {
                            tooltip.append('\n');
                        }
                        tooltip.append("Aggregations: ")
                                .append(String.join(", ", descriptor.providedAggregations()));
                    }
                    setTooltip(tooltip.length() > 0 ? new Tooltip(tooltip.toString()) : null);
                }
            });
            pluginListView.setFocusTraversable(false);
        }

        updatePluginStatus();
    }

    @FXML
    private void onBrowse(ActionEvent event) {
        Window window = browseButton.getScene() != null ? browseButton.getScene().getWindow() : null;
        if (window == null) {
            showWarning("Unable to determine window for file chooser.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select GC log");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GC logs", "*.log", "*.log.*", "*.txt", "*.gz"),
                new FileChooser.ExtensionFilter("All files", "*.*"));

        java.io.File selected = chooser.showOpenDialog(window);
        if (selected != null) {
            logPathField.setText(selected.toPath().toAbsolutePath().toString());
            hideWarning();
            statusLabel.setText("Ready to analyse " + selected.getName());
        }
    }

    @FXML
    private void onAnalyze(ActionEvent event) {
        if (analysisService == null) {
            showWarning("Analysis service not configured.");
            return;
        }

        String pathText = logPathField.getText();
        if (pathText == null || pathText.trim().isEmpty()) {
            showWarning("Please choose a GC log before analysing.");
            return;
        }

        Path path = Path.of(pathText.trim());
        if (!Files.exists(path)) {
            showWarning("Selected log does not exist.");
            return;
        }

        hideWarning();
        statusLabel.setText("Queued analysis for " + path.getFileName());

        Task<AnalysisResult> task = new Task<>() {
            @Override
            protected AnalysisResult call() throws Exception {
                updateMessage("Starting analysis...");
                updateProgress(-1, 1);
                AnalysisResult result = analysisService.analyze(path);
                updateMessage("Finalising results...");
                updateProgress(1, 1);
                return result;
            }
        };

        if (progressController != null) {
            progressController.monitor(task, result -> {
                statusLabel.setText("Analysis completed at " + result.getAnalyzedAt());
                hideWarning();
                if (onAnalysisCompleted != null) {
                    onAnalysisCompleted.accept(result);
                }
            }, throwable -> handleFailure(path, throwable));
        } else {
            task.setOnSucceeded(event1 -> {
                AnalysisResult result = task.getValue();
                statusLabel.setText("Analysis completed at " + result.getAnalyzedAt());
                hideWarning();
                if (onAnalysisCompleted != null) {
                    onAnalysisCompleted.accept(result);
                }
            });
            task.setOnFailed(event12 -> handleFailure(path, task.getException()));
        }

        if (analysisExecutor != null) {
            analysisExecutor.submit(task);
        } else {
            Thread thread = new Thread(task, "gc-analysis-task");
            thread.setDaemon(true);
            thread.start();
        }

        if (progressController != null && progressDialogStage != null) {
            progressDialogStage.show();
        }
    }

    public void setAnalysisService(AnalysisService analysisService) {
        this.analysisService = Objects.requireNonNull(analysisService, "analysisService");
        setPluginRegistry(analysisService.getPluginRegistry());
    }

    public void setPluginRegistry(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
        updatePluginStatus();
    }

    public void setAnalysisProgressController(AnalysisProgressController progressController) {
        this.progressController = progressController;
    }

    public void setProgressDialogStage(Stage progressDialogStage) {
        this.progressDialogStage = progressDialogStage;
    }

    public void setOnAnalysisCompleted(Consumer<AnalysisResult> onAnalysisCompleted) {
        this.onAnalysisCompleted = onAnalysisCompleted;
    }

    public void setOnAnalysisFailed(Consumer<Throwable> onAnalysisFailed) {
        this.onAnalysisFailed = onAnalysisFailed;
    }

    public void setAnalysisExecutor(ExecutorService analysisExecutor) {
        this.analysisExecutor = Objects.requireNonNull(analysisExecutor, "analysisExecutor");
    }

    public void populateLogPath(Path path) {
        Objects.requireNonNull(path, "path");
        logPathField.setText(path.toAbsolutePath().toString());
        statusLabel.setText("Ready to analyse " + (path.getFileName() != null ? path.getFileName() : path));
        hideWarning();
    }

    private void updatePluginStatus() {
        if (pluginSummaryLabel == null || pluginListView == null) {
            return;
        }
        ObservableList<PluginDescriptor> items = pluginListView.getItems();
        if (items == null) {
            pluginListView.setItems(FXCollections.observableArrayList());
            items = pluginListView.getItems();
        }
        if (pluginRegistry == null) {
            items.clear();
            pluginSummaryLabel.setText("Plug-in discovery has not run yet.");
            return;
        }
        items.setAll(pluginRegistry.descriptors());
        long total = pluginRegistry.descriptors().size();
        long active = pluginRegistry.activeCount();
        long failed = pluginRegistry.failureCount();
        StringBuilder summary = new StringBuilder();
        if (total == 0) {
            summary.append("No plug-ins were discovered.");
        } else {
            summary.append(String.format(java.util.Locale.ROOT, "Loaded %d of %d plug-ins.", active, total));
            if (failed > 0) {
                summary.append(' ').append(String.format(java.util.Locale.ROOT, "%d failed to load.", failed));
            }
        }
        pluginSummaryLabel.setText(summary.toString());
    }

    private void handleFailure(Path path, Throwable throwable) {
        Throwable cause = unwrap(throwable);
        String message;
        AnalysisException analysisException = null;
        if (cause instanceof java.util.concurrent.CancellationException) {
            message = "Analysis cancelled.";
        } else if (cause instanceof AnalysisException exception) {
            analysisException = exception;
            message = exception.getFriendlyMessage();
            LOGGER.warn("Analysis failed: {}", exception.describeContext(), exception);
        } else if (cause instanceof IOException) {
            message = "Unable to read GC log: " + path.getFileName();
            LOGGER.error("Failed to read GC log {}", path, cause);
        } else if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            message = "Analysis failed: " + cause.getMessage();
            LOGGER.error("Analysis failed for {}", path, cause);
        } else {
            message = "Analysis failed: unknown error";
            LOGGER.error("Analysis failed for {} with unknown cause", path);
        }
        showWarning(message);
        statusLabel.setText(message);
        if (onAnalysisFailed != null) {
            onAnalysisFailed.accept(cause);
        }
        if (analysisException != null) {
            offerCrashReport(analysisException);
        }
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof ExecutionException executionException && executionException.getCause() != null) {
            return executionException.getCause();
        }
        return throwable;
    }

    private void offerCrashReport(AnalysisException exception) {
        if (crashReportService == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Send diagnostics to support");
        alert.setHeaderText("Generate a crash report?");
        alert.setContentText("We can package the selected log, configuration, and system details. " +
                "No information will be sent without your consent.");
        ButtonType generate = new ButtonType("Generate report", ButtonBar.ButtonData.OK_DONE);
        ButtonType decline = new ButtonType("No thanks", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(generate, decline);

        Optional<ButtonType> response = alert.showAndWait();
        if (response.isPresent() && response.get() == generate) {
            try {
                Path archive = crashReportService.packageForSupport(exception);
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Crash report ready");
                success.setHeaderText("Diagnostics packaged");
                success.setContentText("Attach the archive located at:\n" + archive.toAbsolutePath());
                success.showAndWait();
            } catch (IOException e) {
                LOGGER.error("Unable to package crash report", e);
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Crash report failed");
                errorAlert.setHeaderText("We couldn't package the crash report");
                errorAlert.setContentText(e.getMessage() != null ? e.getMessage() : "Unknown error");
                errorAlert.showAndWait();
            }
        }
    }

    public void setCrashReportService(CrashReportService crashReportService) {
        this.crashReportService = Objects.requireNonNull(crashReportService, "crashReportService");
    }

    private void showWarning(String message) {
        warningBadge.setText(message);
        warningBadge.setManaged(true);
        warningBadge.setVisible(true);
    }

    private void hideWarning() {
        warningBadge.setManaged(false);
        warningBadge.setVisible(false);
    }
}
