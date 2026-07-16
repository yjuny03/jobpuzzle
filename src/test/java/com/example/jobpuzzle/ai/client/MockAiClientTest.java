package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult.*;
import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.analysis.entity.ActionPlanMatchLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MockAiClientTest {

    private final MockAiClient mockAiClient = new MockAiClient();
    // Lombok @Builder만 쓰는 순수 객체라 스프링 없이 new로 바로 만들어도 됨

    @Test
    @DisplayName("공고 분석 결과는 null이 아니다")
    void analyzeJobPosting_결과가_null이_아니다() {
        // given
        String dummyPrompt = "테스트용 프롬프트";

        // when
        JobPostingAnalysisResult result = mockAiClient.analyzeJobPosting(dummyPrompt);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMainTasks()).isNotEmpty();
        assertThat(result.getRequirements()).isNotEmpty();
    }

    @Test
    @DisplayName("지원자 분석 결과는 null이 아니고, 중첩 객체와 리스트가 최소 1개 이상 존재한다")
    void analyzeCandidateMaterial_결과가_null이_아니다() {
        CandidateMaterialAnalysisResult result = mockAiClient.analyzeCandidateMaterial("테스트용 프롬프트");

        assertThat(result).isNotNull();
        assertThat(result.getResume()).isNotNull();
        assertThat(result.getResume().getExperiences()).isNotEmpty();
        assertThat(result.getCoverLetter()).isNotNull();
        assertThat(result.getPortfolio()).isNotNull();
        assertThat(result.getPortfolio().getProjectStructure()).isNotEmpty();
        assertThat(result.getExperienceNote()).isNotNull();
        assertThat(result.getExperienceNote().getStarCandidates()).isNotEmpty();
    }

    @Test
    @DisplayName("Provider는 MOCK이다")
    void getProvider_MOCK을_반환한다() {
        AiProvider provider = mockAiClient.getProvider();

        assertThat(provider).isEqualTo(AiProvider.MOCK);
    }

    @Test
    @DisplayName("과제(Task)에는 HIGH 등급이 존재하지 않는다")
    void tasks에_HIGH가_없다() {
        QuestionGenerationResult result = mockAiClient.generateQuestions("테스트용 프롬프트");

        List<String> matchLevelNames = result.getTasks().stream()
                .map(task -> task.getMatchLevel().name())
                .collect(Collectors.toList());

        assertThat(matchLevelNames).doesNotContain("HIGH");
    }

    @Test
    @DisplayName("과제의 relatedRequirement는 적합도 목록에 실제로 존재한다")
    void 과제가_적합도_목록에_연결된다() {
        QuestionGenerationResult result = mockAiClient.generateQuestions("테스트용 프롬프트");

        List<String> requirementList = result.getRequirementMatches().stream()
                .map(RequirementMatch::getRequirement)
                .collect(Collectors.toList());

        assertThat(result.getTasks()).isNotEmpty();
        result.getTasks().forEach(task ->
                assertThat(requirementList).contains(task.getRelatedRequirement())
        );
    }

    @Test
    @DisplayName("질문은 관련 요구사항에 연결된다")
    void 질문이_적합도_목록에_연결된다() {
        QuestionGenerationResult result = mockAiClient.generateQuestions("테스트용 프롬프트");

        List<String> requirementList = result.getRequirementMatches().stream()
                .map(RequirementMatch::getRequirement)
                .collect(Collectors.toList());

        assertThat(result.getQuestions()).isNotEmpty();
        result.getQuestions().forEach(question ->
                assertThat(requirementList).contains(question.getRelatedRequirement())
        );
    }
}
