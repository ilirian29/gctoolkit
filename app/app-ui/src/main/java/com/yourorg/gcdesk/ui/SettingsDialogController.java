package com.yourorg.gcdesk.ui;

import com.yourorg.gcdesk.preferences.Theme;
import com.yourorg.gcdesk.preferences.UserPreferences;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Controller for the preferences dialog, allowing users to adjust global settings.
 */
public class SettingsDialogController {

    @FXML
    private ChoiceBox<Theme> themeChoice;

    @FXML
    private TextField exportDirectoryField;

    @FXML
    private Spinner<Integer> concurrencySpinner;

    @FXML
    private Label errorLabel;

    private UserPreferences preferences;

    @FXML
    private void initialize() {
        themeChoice.getItems().setAll(Theme.values());
        concurrencySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 64, 1));
        concurrencySpinner.setEditable(true);
        clearError();
    }

    public void setPreferences(UserPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        themeChoice.getSelectionModel().select(preferences.theme());
        exportDirectoryField.setText(preferences.resolveExportDirectory().toString());
        concurrencySpinner.getValueFactory().setValue(preferences.analysisConcurrency());
    }

    @FXML
    private void onBrowseExport() {
        Window window = exportDirectoryField.getScene() != null ? exportDirectoryField.getScene().getWindow() : null;
        if (window == null) {
            showError("Unable to open a directory chooser.");
            return;
        }

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select export directory");
        Path current = resolveExportDirectory();
        if (current != null && Files.exists(current)) {
            chooser.setInitialDirectory(current.toFile());
        }
        java.io.File directory = chooser.showDialog(window);
        if (directory != null) {
            exportDirectoryField.setText(directory.getAbsolutePath());
            clearError();
        }
    }

    public Optional<UserPreferences> buildUpdatedPreferences() {
        if (preferences == null) {
            return Optional.empty();
        }
        Theme selectedTheme = themeChoice.getValue() != null ? themeChoice.getValue() : Theme.SYSTEM;
        Path exportDirectory = resolveExportDirectory();
        if (exportDirectory == null) {
            showError("Choose an export directory.");
            return Optional.empty();
        }
        try {
            Files.createDirectories(exportDirectory);
        } catch (IOException ex) {
            showError("Unable to create export directory: " + ex.getMessage());
            return Optional.empty();
        }

        Integer concurrencyValue = concurrencySpinner.getValue();
        if (concurrencyValue == null || concurrencyValue < 1) {
            showError("Concurrency must be at least 1.");
            return Optional.empty();
        }

        clearError();
        UserPreferences updated = preferences
                .withTheme(selectedTheme)
                .withDefaultExportDirectory(exportDirectory.toString())
                .withAnalysisConcurrency(concurrencyValue);
        return Optional.of(updated);
    }

    private Path resolveExportDirectory() {
        String value = exportDirectoryField.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        return Path.of(value.trim());
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setText("");
    }
}
