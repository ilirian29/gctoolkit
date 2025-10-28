package com.yourorg.gcdesk.ui;

import com.example.app.core.reporting.ReportFormat;
import com.example.app.core.reporting.ReportGenerationException;
import com.example.app.core.reporting.ReportService;
import com.yourorg.gcdesk.model.AnalysisResult;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class GCDeskApplicationTest extends ApplicationTest {

    private static final Path SAMPLE_LOG = Path.of("..", "..", "gclogs", "samples", "g1-sample.log")
            .toAbsolutePath().normalize();

    private LogSelectionController logSelectionController;
    private AnalysisProgressController progressController;
    private ResultsDashboardController resultsController;
    private Stage progressStage;
    private RecordingReportService reportService;

    @BeforeAll
    static void configureHeadless() {
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "true");
        Locale.setDefault(Locale.US);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader selectionLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/log-selection.fxml"));
        Parent logSelectionView = selectionLoader.load();
        logSelectionController = selectionLoader.getController();

        FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/results-dashboard.fxml"));
        Parent dashboardView = dashboardLoader.load();
        resultsController = dashboardLoader.getController();

        FXMLLoader progressLoader = new FXMLLoader(getClass().getResource("/com/yourorg/gcdesk/ui/analysis-progress.fxml"));
        Parent progressView = progressLoader.load();
        progressController = progressLoader.getController();

        progressStage = new Stage();
        progressStage.initOwner(stage);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.setScene(new Scene(progressView));
        progressStage.setTitle("Analyzing GC Log");
        progressStage.setResizable(false);
        progressController.setOnDismiss(progressStage::hide);

        logSelectionController.setAnalysisProgressController(progressController);
        logSelectionController.setProgressDialogStage(progressStage);
        logSelectionController.setOnAnalysisFailed(throwable -> progressStage.hide());

        reportService = new RecordingReportService();
        resultsController.setReportService(reportService);
        resultsController.setDirectoryChooserFactory(() -> new StubDirectoryChooser(reportService));

        logSelectionController.setOnAnalysisCompleted(resultsController::displayResult);

        javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
        root.setTop(logSelectionView);
        root.setCenter(dashboardView);
        stage.setScene(new Scene(root, 1200, 800));
        stage.setTitle("GC Desk Test Harness");
        stage.show();
    }

    @Test
    void runAnalysisFlowDisplaysResults(FxRobot robot) {
        interact(() -> {
            logSelectionController.setAnalysisService(new com.yourorg.gcdesk.AnalysisService());
            logSelectionController.setOnAnalysisCompleted(resultsController::displayResult);
            robot.lookup("#logPathField").<javafx.scene.control.TextField>query().setText("");
        });

        robot.clickOn("#logPathField");
        robot.write(SAMPLE_LOG.toString());
        robot.clickOn("#analyzeButton");

        try {
            WaitForAsyncUtils.waitFor(30, TimeUnit.SECONDS, () -> {
                String text = robot.lookup("#sourceLabel").queryLabeled().getText();
                return SAMPLE_LOG.toString().equals(text);
            });
        } catch (TimeoutException ex) {
            throw new AssertionError("Timed out waiting for analysis to complete", ex);
        }

        try {
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> !robot.lookup("#causeTable")
                    .<javafx.scene.control.TableView<?>>query().getItems().isEmpty());
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> !robot.lookup("#cycleTable")
                    .<javafx.scene.control.TableView<?>>query().getItems().isEmpty());
        } catch (TimeoutException ex) {
            throw new AssertionError("Timed out waiting for results tables to populate", ex);
        }

        String statusText = robot.lookup("#statusLabel").queryLabeled().getText();
        assertThat(statusText).contains("Analysis completed");
        assertThat(robot.lookup("#sourceLabel").queryLabeled().getText())
                .isEqualTo(SAMPLE_LOG.toString());
        assertThat(robot.lookup("#causeTable").<javafx.scene.control.TableView<?>>query().getItems())
                .as("cause table rows")
                .isNotEmpty();
        assertThat(robot.lookup("#cycleTable").<javafx.scene.control.TableView<?>>query().getItems())
                .as("cycle table rows")
                .isNotEmpty();
    }

    @Test
    void exportReportCreatesFile(FxRobot robot) {
        runAnalysisFlowDisplaysResults(robot);

        robot.clickOn("#exportButton");

        try {
            WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> reportService.getLastReportPath() != null);
        } catch (TimeoutException ex) {
            throw new AssertionError("Timed out waiting for export to finish", ex);
        }
        Path exported = reportService.getLastReportPath();
        assertThat(exported).isNotNull();
        assertThat(Files.exists(exported)).isTrue();
    }

    private static class StubDirectoryChooser implements ResultsDashboardController.DirectoryChooserAdapter {
        private final RecordingReportService reportService;

        StubDirectoryChooser(RecordingReportService reportService) {
            this.reportService = reportService;
        }

        @Override
        public void setTitle(String title) {
            // no-op for tests
        }

        @Override
        public java.io.File showDialog(javafx.stage.Window ownerWindow) {
            try {
                Path tempDir = Files.createTempDirectory("gcdesk-report");
                reportService.setExportDirectory(tempDir);
                return tempDir.toFile();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public void setInitialDirectory(java.io.File directory) {
            // no-op for tests
        }
    }

    private static class RecordingReportService extends ReportService {
        private final AtomicReference<Path> exportDirectory = new AtomicReference<>();
        private final AtomicReference<Path> lastReport = new AtomicReference<>();

        void setExportDirectory(Path directory) {
            exportDirectory.set(directory);
        }

        Path getLastReportPath() {
            return lastReport.get();
        }

        @Override
        public Path generateReport(AnalysisResult result, Path outputDirectory, ReportFormat format) throws ReportGenerationException {
            Path directory = exportDirectory.get();
            Path targetDir = directory != null ? directory : outputDirectory;
            try {
                Files.createDirectories(targetDir);
                Path report = targetDir.resolve("report." + format.getFileExtension());
                Files.writeString(report, "test report");
                lastReport.set(report);
                return report;
            } catch (IOException e) {
                throw new ReportGenerationException("Unable to write test report", e);
            }
        }
    }
}
