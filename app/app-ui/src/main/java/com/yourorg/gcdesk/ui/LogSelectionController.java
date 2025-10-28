package com.yourorg.gcdesk.ui;

import com.yourorg.gcdesk.AnalysisService;
import com.yourorg.gcdesk.model.AnalysisResult;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Controller backing the log selection screen. It validates user input, surfaces warnings and
 * orchestrates execution of the {@link AnalysisService} while delegating progress reporting and
 * result rendering to collaborators.
 */
public class LogSelectionController {

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

    private AnalysisService analysisService = new AnalysisService();
    private AnalysisProgressController progressController;
    private Stage progressDialogStage;
    private Consumer<AnalysisResult> onAnalysisCompleted;
    private Consumer<Throwable> onAnalysisFailed;

    @FXML
    private void initialize() {
        warningBadge.setManaged(false);
        warningBadge.setVisible(false);
        statusLabel.setText("Select a GC log to begin.");
        analyzeButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> logPathField.getText() == null || logPathField.getText().trim().isEmpty(),
                logPathField.textProperty()));
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

        Thread thread = new Thread(task, "gc-analysis-task");
        thread.setDaemon(true);
        thread.start();

        if (progressController != null && progressDialogStage != null) {
            progressDialogStage.show();
        }
    }

    public void setAnalysisService(AnalysisService analysisService) {
        this.analysisService = Objects.requireNonNull(analysisService, "analysisService");
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

    private void handleFailure(Path path, Throwable throwable) {
        String message;
        if (throwable instanceof java.util.concurrent.CancellationException) {
            message = "Analysis cancelled.";
        } else if (throwable instanceof IOException) {
            message = "Unable to read GC log: " + path.getFileName();
        } else if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            message = "Analysis failed: " + throwable.getMessage();
        } else {
            message = "Analysis failed: unknown error";
        }
        showWarning(message);
        statusLabel.setText(message);
        if (onAnalysisFailed != null) {
            onAnalysisFailed.accept(throwable);
        }
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
