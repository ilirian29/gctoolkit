package com.yourorg.gcdesk.ui;

import com.example.app.core.reporting.ReportService;
import com.yourorg.gcdesk.AnalysisService;
import com.yourorg.gcdesk.CrashReportService;
import com.yourorg.gcdesk.preferences.PreferencesService;
import com.yourorg.gcdesk.preferences.Theme;
import com.yourorg.gcdesk.preferences.UserPreferences;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaFX entry point that stitches together the log selection view, progress dialog and
 * results dashboard into a cohesive desktop experience.
 */
public class GCDeskApplication extends Application {

    private final AtomicInteger threadCounter = new AtomicInteger();
    private PreferencesService preferencesService;
    private UserPreferences preferences;
    private ExecutorService analysisExecutor;
    private Scene scene;
    private LogSelectionController logSelectionController;
    private ResultsDashboardController dashboardController;
    private WorkspaceController workspaceController;

    @Override
    public void start(Stage primaryStage) throws IOException {
        preferencesService = new PreferencesService();
        preferences = preferencesService.load();
        analysisExecutor = createExecutor(preferences.analysisConcurrency());

        FXMLLoader selectionLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/log-selection.fxml"));
        Parent logSelectionView = selectionLoader.load();
        logSelectionController = selectionLoader.getController();

        FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/results-dashboard.fxml"));
        Parent dashboardView = dashboardLoader.load();
        dashboardController = dashboardLoader.getController();

        FXMLLoader progressLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/analysis-progress.fxml"));
        Parent progressView = progressLoader.load();
        AnalysisProgressController progressController = progressLoader.getController();

        FXMLLoader workspaceLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/workspace.fxml"));
        Parent workspaceView = workspaceLoader.load();
        workspaceController = workspaceLoader.getController();

        Stage progressStage = new Stage();
        progressStage.initOwner(primaryStage);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setScene(new Scene(progressView));
        progressStage.setTitle("Analyzing GC Log");
        progressStage.setResizable(false);

        progressController.setOnDismiss(progressStage::hide);

        AnalysisService analysisService = new AnalysisService();
        CrashReportService crashReportService = new CrashReportService();
        ReportService reportService = new ReportService();
        logSelectionController.setAnalysisService(analysisService);
        logSelectionController.setCrashReportService(crashReportService);
        logSelectionController.setAnalysisProgressController(progressController);
        logSelectionController.setProgressDialogStage(progressStage);
        logSelectionController.setAnalysisExecutor(analysisExecutor);
        logSelectionController.setOnAnalysisCompleted(result -> {
            Objects.requireNonNull(result, "result");
            dashboardController.displayResult(result);
            workspaceController.recordAnalysis(result.getSource(), result.getAnalyzedAt());
        });
        logSelectionController.setOnAnalysisFailed(throwable -> progressStage.hide());
        dashboardController.setReportService(reportService);
        dashboardController.setDefaultExportDirectory(preferences.resolveExportDirectory());

        workspaceController.setPreferencesService(preferencesService);
        workspaceController.setPreferences(preferences);
        workspaceController.setOnOpenRecent(path -> logSelectionController.populateLogPath(path));

        BorderPane root = new BorderPane();
        MenuBar menuBar = buildMenuBar(primaryStage);
        root.setTop(new VBox(menuBar, logSelectionView));
        root.setLeft(workspaceView);
        root.setCenter(dashboardView);

        scene = new Scene(root, 1200, 800);
        applyTheme(preferences.theme());
        primaryStage.setScene(scene);
        primaryStage.setTitle("GC Desk");
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (analysisExecutor != null) {
            analysisExecutor.shutdownNow();
        }
    }

    private MenuBar buildMenuBar(Stage owner) {
        MenuBar menuBar = new MenuBar();
        Menu settingsMenu = new Menu("Settings");
        MenuItem preferencesItem = new MenuItem("Preferences...");
        preferencesItem.setOnAction(event -> showPreferencesDialog(owner));
        settingsMenu.getItems().add(preferencesItem);

        Menu helpMenu = new Menu("Help");
        MenuItem userManualItem = new MenuItem("User Manual");
        userManualItem.setOnAction(event -> showHelpDocument(owner, "/com/yourorg/gcdesk/ui/help/user-manual.html", "User Manual"));
        MenuItem tutorialsItem = new MenuItem("Guided Tutorials");
        tutorialsItem.setOnAction(event -> showHelpDocument(owner, "/com/yourorg/gcdesk/ui/help/guided-tutorials.html", "Guided Tutorials"));
        MenuItem onlineDocsItem = new MenuItem("Open Online Docs");
        onlineDocsItem.setOnAction(event -> getHostServices().showDocument("https://yourorg.github.io/gctoolkit/"));
        helpMenu.getItems().addAll(userManualItem, tutorialsItem, onlineDocsItem);

        menuBar.getMenus().addAll(settingsMenu, helpMenu);
        return menuBar;
    }

    private void showHelpDocument(Stage owner, String resourcePath, String title) {
        URL resourceUrl = getClass().getResource(resourcePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Missing help document: " + resourcePath);
        }
        WebView webView = new WebView();
        webView.getEngine().load(resourceUrl.toExternalForm());

        Stage helpStage = new Stage();
        helpStage.initOwner(owner);
        helpStage.setTitle(title);
        helpStage.setScene(new Scene(webView, 900, 700));
        helpStage.show();
    }

    private void showPreferencesDialog(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/settings-dialog.fxml"));
            DialogPane dialogPane = loader.load();
            SettingsDialogController controller = loader.getController();
            controller.setPreferences(preferences);

            dialogPane.getStylesheets().clear();
            dialogPane.getStylesheets().add("/com/yourorg/gcdesk/ui/styles.css");
            if (preferences.theme() == Theme.DARK) {
                dialogPane.getStylesheets().add("/com/yourorg/gcdesk/ui/dark-styles.css");
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.initOwner(owner);

            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            okButton.addEventFilter(ActionEvent.ACTION, event -> {
                Optional<UserPreferences> maybe = controller.buildUpdatedPreferences();
                if (maybe.isPresent()) {
                    applyPreferences(maybe.get());
                } else {
                    event.consume();
                }
            });

            dialog.showAndWait();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load preferences dialog", ex);
        }
    }

    private void applyPreferences(UserPreferences updated) {
        preferences = preferencesService.save(updated);
        reconfigureExecutor(preferences.analysisConcurrency());
        applyTheme(preferences.theme());
        workspaceController.setPreferences(preferences);
        dashboardController.setDefaultExportDirectory(preferences.resolveExportDirectory());
    }

    private void applyTheme(Theme theme) {
        scene.getStylesheets().clear();
        scene.getStylesheets().add("/com/yourorg/gcdesk/ui/styles.css");
        if (theme == Theme.DARK) {
            scene.getStylesheets().add("/com/yourorg/gcdesk/ui/dark-styles.css");
        }
    }

    private ExecutorService createExecutor(int concurrency) {
        int poolSize = Math.max(1, concurrency);
        return Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName("gc-analysis-" + threadCounter.incrementAndGet());
            return thread;
        });
    }

    private void reconfigureExecutor(int concurrency) {
        if (analysisExecutor != null) {
            analysisExecutor.shutdownNow();
        }
        analysisExecutor = createExecutor(concurrency);
        logSelectionController.setAnalysisExecutor(analysisExecutor);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
