package com.example.app.core.reporting;

import com.microsoft.gctoolkit.event.GCCause;
import com.yourorg.gcdesk.model.AnalysisResult;
import com.yourorg.gcdesk.testing.AnalysisFixtures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTemplateRendererTest {

    private static AnalysisResult analysisResult;

    @BeforeAll
    static void loadAnalysis() {
        analysisResult = AnalysisFixtures.analyseSampleLog();
    }

    @Test
    void renderedReportContainsKeySections() throws ReportGenerationException {
        ReportViewModelMapper mapper = new ReportViewModelMapper();
        ReportTemplateRenderer renderer = new ReportTemplateRenderer();
        ReportViewModel model = mapper.map(analysisResult, analysisResult.getAnalyzedAt());

        String html = renderer.render(model);

        assertThat(html)
                .as("HTML report title")
                .contains("GC Analysis Report");
        assertThat(html)
                .as("summary table headings")
                .contains("Total pause time")
                .contains("Percent paused")
                .contains("GC cause breakdown")
                .contains("Collection cycle counts");
    }

    @Test
    void recommendationsIncludeDominantCause() throws ReportGenerationException {
        ReportViewModelMapper mapper = new ReportViewModelMapper();
        ReportViewModel model = mapper.map(analysisResult, analysisResult.getAnalyzedAt());

        Map<GCCause, Integer> causeCounts =
                analysisResult.getGcCauseSummary().getCauseCounts();

        String topCauseLabel = causeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().getLabel())
                .orElseThrow(() -> new AssertionError("Expected at least one GC cause"));

        assertThat(model.getRecommendations())
                .as("recommendations")
                .isNotEmpty()
                .anySatisfy(rec -> assertThat(rec.getDetail()).contains(topCauseLabel));
        assertThat(model.getCauseRows())
                .as("cause rows")
                .anySatisfy(row -> assertThat(row.getCause()).isEqualTo(topCauseLabel));
    }
}
