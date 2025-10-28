package com.yourorg.gcdesk.ui;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * Controller that drives the analysis progress dialog, binding to an asynchronous task and surfacing
 * success, failure and cancellation information via warning badges.
 */
public class AnalysisProgressController {

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label messageLabel;

    @FXML
    private Label warningBadge;

    @FXML
    private Button cancelButton;

    private Task<?> currentTask;
    private Runnable onDismiss;

    @FXML
    private void initialize() {
        hideWarning();
        progressBar.setProgress(0);
        cancelButton.setDisable(true);
    }

    /**
     * Bind the UI to the supplied task, updating the message and progress as the computation advances.
     *
     * @param task       asynchronous task performing the log analysis
     * @param onSuccess  callback invoked when the task completes successfully
     * @param onFailure  callback invoked when the task fails or is cancelled
     * @param <T>        task result type
     */
    public <T> void monitor(Task<T> task, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Objects.requireNonNull(task, "task");
        currentTask = task;
        hideWarning();

        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        messageLabel.textProperty().unbind();
        messageLabel.textProperty().bind(task.messageProperty());
        cancelButton.disableProperty().unbind();
        cancelButton.disableProperty().bind(task.runningProperty().not());

        task.stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case SUCCEEDED -> handleSuccess(task, onSuccess);
                case FAILED -> handleFailure(task.getException(), onFailure);
                case CANCELLED -> handleFailure(new CancellationException("Analysis cancelled"), onFailure);
                default -> {
                }
            }
        });
    }

    @FXML
    private void onCancel(ActionEvent event) {
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel(true);
        }
    }

    public void setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }

    private <T> void handleSuccess(Task<T> task, Consumer<T> onSuccess) {
        progressBar.progressProperty().unbind();
        messageLabel.textProperty().unbind();
        cancelButton.disableProperty().unbind();
        cancelButton.setDisable(true);
        messageLabel.setText("Analysis complete");
        hideWarning();
        if (onSuccess != null) {
            onSuccess.accept(task.getValue());
        }
        dismiss();
    }

    private void handleFailure(Throwable throwable, Consumer<Throwable> onFailure) {
        progressBar.progressProperty().unbind();
        messageLabel.textProperty().unbind();
        cancelButton.disableProperty().unbind();
        cancelButton.setDisable(true);
        boolean cancelled = throwable instanceof CancellationException;
        messageLabel.setText(cancelled ? "Analysis cancelled" : "Analysis failed");
        String message = throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()
                ? throwable.getMessage()
                : (cancelled ? "Analysis cancelled" : "Unknown error");
        showWarning(message);
        if (onFailure != null) {
            onFailure.accept(throwable);
        }
        dismiss();
    }

    private void dismiss() {
        if (onDismiss != null) {
            onDismiss.run();
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
