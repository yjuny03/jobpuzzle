package com.example.jobpuzzle.ai.prompt;

import com.example.jobpuzzle.ai.log.AiCallLogErrorType;
import com.example.jobpuzzle.ai.log.AiExecutionStage;
import com.example.jobpuzzle.ai.validation.AiProcessingException;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContext;
import com.example.jobpuzzle.analysis.dto.AnalysisInputSnapshotContextSource;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class PromptTemplateRendererTest {

    private final PromptTemplateRenderer renderer = new PromptTemplateRenderer();

    @Test
    void rendersAllJson01VariablesWithoutChangingSourceMarker() {
        PromptTemplate template = template("""
                {{mainCategory}}|{{subCategory}}|{{careerLevel}}
                {{jobPostingAnalysisText}}
                {{companyInfoAnalysisTexts}}
                """);
        String prompt = renderer.render(AiExecutionStage.JOB_POSTING_ANALYSIS, template, context(), List.of(source(UserDocumentType.JOB_POSTING)), List.of());

        assertThat(prompt).contains("IT", "BACKEND", "NEW", "[SOURCE extractionId=1 documentId=2][PAGE=1][SEGMENT=seg-001]");
    }

    @Test
    void rejectsMissingOrUnknownTemplateVariable() {
        assertRenderFailure(template("{{mainCategory}}"));
        assertRenderFailure(template("{{mainCategory}}{{subCategory}}{{careerLevel}}{{jobPostingAnalysisText}}{{companyInfoAnalysisTexts}}{{unknown}}"));
    }

    private void assertRenderFailure(PromptTemplate template) {
        assertThatThrownBy(() -> renderer.render(AiExecutionStage.JOB_POSTING_ANALYSIS, template, context(), List.of(source(UserDocumentType.JOB_POSTING)), List.of()))
                .isInstanceOf(AiProcessingException.class)
                .extracting(error -> ((AiProcessingException) error).getErrorType())
                .isEqualTo(AiCallLogErrorType.PROMPT_RENDER_FAILED);
    }

    private PromptTemplate template(String text) {
        return PromptTemplate.builder().promptCode("PT").name("test").version("v1").targetJson("JSON-01").templateText(text).isActive(true).build();
    }

    private AnalysisInputSnapshotContext context() {
        AnalysisInputSnapshotContext context = Mockito.mock(AnalysisInputSnapshotContext.class);
        when(context.getMainCategory()).thenReturn("IT");
        when(context.getSubCategory()).thenReturn("BACKEND");
        when(context.getCareerLevel()).thenReturn(JobCategoryCareerLevel.NEW);
        return context;
    }

    private AnalysisInputSnapshotContextSource source(UserDocumentType type) {
        AnalysisInputSnapshotContextSource source = Mockito.mock(AnalysisInputSnapshotContextSource.class);
        when(source.getDocumentType()).thenReturn(type);
        when(source.getDisplayName()).thenReturn("자료");
        when(source.getAnalysisText()).thenReturn("[SOURCE extractionId=1 documentId=2][PAGE=1][SEGMENT=seg-001]\n본문");
        return source;
    }
}
