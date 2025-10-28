package com.yourorg.gcdesk.ui;

import com.example.app.core.reporting.ReportService;
import com.yourorg.gcdesk.AnalysisService;
import com.yourorg.gcdesk.CrashReportService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX entry point that stitches together the log selection view, progress dialog and
 * results dashboard into a cohesive desktop experience.
 */
public class GCDeskApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader selectionLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/log-selection.fxml"));
        Parent logSelectionView = selectionLoader.load();
        LogSelectionController logSelectionController = selectionLoader.getController();

        FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/results-dashboard.fxml"));
        Parent dashboardView = dashboardLoader.load();
        ResultsDashboardController dashboardController = dashboardLoader.getController();

        FXMLLoader progressLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/analysis-progress.fxml"));
        Parent progressView = progressLoader.load();
        AnalysisProgressController progressController = progressLoader.getController();

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
        logSelectionController.setOnAnalysisCompleted(result -> {
            Objects.requireNonNull(result, "result");
            dashboardController.displayResult(result);
        });
        logSelectionController.setOnAnalysisFailed(throwable -> progressStage.hide());
        dashboardController.setReportService(reportService);

        BorderPane root = new BorderPane();
        root.setTop(logSelectionView);
        root.setCenter(dashboardView);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("GC Desk");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
