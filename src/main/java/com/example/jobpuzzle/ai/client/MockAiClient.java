package com.example.jobpuzzle.ai.client;

import com.example.jobpuzzle.ai.dto.AnswerEvaluationResult;
import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult.*;
import com.example.jobpuzzle.ai.dto.FinalReportResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult;
import com.example.jobpuzzle.ai.dto.QuestionGenerationResult.*;
import com.example.jobpuzzle.ai.dto.WeaknessAnswerEvaluationResult;
import com.example.jobpuzzle.ai.log.AiProvider;
import com.example.jobpuzzle.analysis.entity.ActionPlanMatchLevel;
import com.example.jobpuzzle.analysis.entity.MatchAnalysisResultMatchLevel;
import com.example.jobpuzzle.analysis.entity.ReadinessResultStatus;
import com.example.jobpuzzle.interview.entity.FollowUpQuestionType;
import com.example.jobpuzzle.interview.entity.InterviewQuestionReviewStatus;
import com.example.jobpuzzle.interview.entity.InterviewQuestionType;
import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.jobcategory.entity.JobCategoryCareerLevel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockAiClient implements AiClient {

    // 채용공고 내용을 분석하여 구조화된 공고 분석 결과를 반환
    @Override
    public JobPostingAnalysisResult analyzeJobPosting(String prompt) {
        return JobPostingAnalysisResult.builder()
                .mainTasks(List.of("백엔드 API 설계 및 개발"
                        ,"DB 스키마 설계"
                ))
                .requirements(List.of("Spring Boot 기반 개발 경험"
                        , "RDB 설계 경험"
                ))
                .preferred(List.of("AWS 인프라 경험"
                        , "테스트 코드 작성 경험"
                ))
                .companyValues(List.of("주도적 문제 해결",
                        "협업 커뮤니케이션"
                ))
                .coreCompetencies(List.of("API 설계",
                        "장애 대응"
                ))
                .missingEvidence(List.of())
                .build();
    }

    // 이력서·자기소개서 등 지원자 자료를 분석하여 구조화된 분석 결과를 반환
    @Override
    public CandidateMaterialAnalysisResult analyzeCandidateMaterial(String prompt) {
        return CandidateMaterialAnalysisResult.builder()
                .resume(Resume.builder()
                        .experiences(List.of(Experience.builder()
                                        .title("학습 관리 플랫폼 팀 프로젝트")
                                        .period("2025.03-2025.06")
                                .build()))
                        .skills(List.of("Spring Boot", "MySQL", "JPA"))
                        .roles(List.of("백엔드 API 개발","DB 설계"))
                        .results(List.of("회원가입 기능 구현 완료"))
                        .build())
                .coverLetter(CoverLetter.builder()
                        .motivation("백엔드 개발 직무 지원 동기 요약")
                        .values("문제를 구조적으로 해결하는 것을 중요하게 생각함")
                        .experienceNarratives(List.of("팀 프로젝트 중 API 설계 경험 서술"))
                        .jobConnection("공고의 백엔드 개발 업무와 프로젝트 경험 연결 서술")
                        .build())
                .portfolio(Portfolio.builder()
                        .projectStructure(List.of(ProjectStructure.builder()
                                        .projectName("학습 관리 플랫폼")
                                        .role("백엔드")
                                .build()))
                        .contributions(List.of("회원가입/로그인 API 구현"))
                        .techUsageReason(List.of("인증 및 인가 처리를 위해 Spring Security 사용"))
                        .outputs(List.of("Swagger 기반 API 문서"))
                        .build())
                .experienceNote(ExperienceNote.builder()
                        .starCandidates(List.of(StarCandidate.builder()
                                        .situation("트래픽 증가로 응답 지연 발생")
                                        .task("조회 성능 개선")
                                .build()))
                        .build())
                .missingEvidence(List.of())
                .build();
    }

    // 확정된 분석 정보를 바탕으로 적합도, 면접 질문, 보완 과제를 생성
    @Override
    public QuestionGenerationResult generateQuestions(String prompt) {
        // readiness, requirementMatches, questions, tasks를 고정값으로 반환
        return QuestionGenerationResult.builder()
                .readiness(Readiness.builder()
                        .status(ReadinessResultStatus.PARTIAL)
                        .reason("Spring Boot 개발 경험은 확인되지만 AWS 인프라 운영 경험에 대한 근거가 부족함")
                        .build())

                .requirementMatches(List.of(
                        RequirementMatch.builder()
                                .requirement("Spring Boot 기반 백엔드 개발 경험")
                                .candidateEvidence("팀 프로젝트에서 Spring Boot를 사용하여 회원가입 및 로그인 API를 구현함")
                                .matchLevel(MatchAnalysisResultMatchLevel.HIGH)
                                .reason("요구사항과 직접 연결되는 프로젝트 경험 및 담당 기능이 확인됨")
                                .missingPoint("")
                                .build(),

                        RequirementMatch.builder()
                                .requirement("AWS 인프라 운영 경험")
                                .candidateEvidence("제출 자료에서 관련 경험을 확인할 수 없음")
                                .matchLevel(MatchAnalysisResultMatchLevel.NONE)
                                .reason("AWS 배포 또는 운영 경험에 대한 구체적인 근거가 없음")
                                .missingPoint("AWS를 활용한 배포·운영 경험 및 담당 역할")
                                .build()
                ))

                .questions(List.of(
                        Question.builder()
                                .questionId("q-001")
                                .questionType(InterviewQuestionType.EXPERIENCE)
                                .question("Spring Boot로 개발한 API 중 본인이 담당한 기능과 구현 과정을 설명해주세요.")
                                .intent("지원자의 실제 담당 범위와 기술 활용 수준 확인")
                                .evaluationFocus(List.of(
                                        "본인 담당 역할",
                                        "구현 과정",
                                        "문제 해결 경험"
                                ))
                                .relatedRequirement("Spring Boot 기반 백엔드 개발 경험")
                                .reviewStatus(InterviewQuestionReviewStatus.PASS)
                                .reviewNote("")
                                .build()
                ))

                .tasks(List.of(
                        ActionPlanItem.builder()
                                .taskId("t-001")
                                .relatedRequirement("AWS 인프라 운영 경험")
                                .matchLevel(ActionPlanMatchLevel.NONE)
                                .missingPoint("AWS를 활용한 배포·운영 경험 및 담당 역할")
                                .suggestion("AWS를 활용한 배포 실습을 진행하고 사용 서비스, 배포 과정, 발생 문제와 해결 내용을 포트폴리오에 추가")
                                .build()
                ))
                .build();
    }

    // 원 질문·꼬리질문 답변을 평가하여 JSON-06 형식으로 반환
    @Override
    public AnswerEvaluationResult evaluateAnswer(String prompt) {
        return AnswerEvaluationResult.builder()
                .interviewMode(InterviewSessionMode.COMPANY_FIT)
                .currentFollowUpDepth(0)
                .score(76)
                .scoreLabel("현재 답변의 기준 충족도")
                .passThreshold(70)
                .evaluationDetail(AnswerEvaluationResult.EvaluationDetail.builder()
                        .intentMatch(AnswerEvaluationResult.DimensionScore.builder()
                                .score(80).comment("질문 의도에 맞게 답변함").build())
                        .specificity(AnswerEvaluationResult.DimensionScore.builder()
                                .score(70).comment("행동 과정이 다소 추상적임").build())
                        .ownRole(AnswerEvaluationResult.DimensionScore.builder()
                                .score(85).comment("담당 범위가 명확함").build())
                        .problemSolving(AnswerEvaluationResult.DimensionScore.builder()
                                .score(65).comment("해결 과정 설명이 부족함").build())
                        .resultExpression(AnswerEvaluationResult.DimensionScore.builder()
                                .score(60).comment("결과 표현이 부족함").build())
                        .requirementConnection(AnswerEvaluationResult.DimensionScore.builder()
                                .score(78).comment("공고 요구사항과 연결됨").build())
                        .guideAlignment(AnswerEvaluationResult.DimensionScore.builder()
                                .score(72).comment("기술 선택 이유가 부족함").build())
                        .deliveryClarity(AnswerEvaluationResult.DimensionScore.builder()
                                .score(82).comment("답변 구조가 이해하기 쉬움").build())
                        .build())
                .weaknessTags(List.of("RESULT_EXPRESSION_WEAK", "PROBLEM_SOLVING_WEAK"))
                .summary("본인 역할은 명확하나 해결 과정과 결과 표현을 보완해야 함")
                .improvementDirection(List.of("문제 해결 단계와 결과를 구체적으로 설명"))
                .followUp(AnswerEvaluationResult.FollowUp.builder()
                        .depth(1)
                        .question("가장 어려웠던 문제와 해결 단계를 설명해주세요.")
                        .type(FollowUpQuestionType.SPECIFICITY)
                        .targetWeakness("PROBLEM_SOLVING_WEAK")
                        .reason("해결 과정의 구체적인 근거가 부족함")
                        .build())
                .build();
    }

    // 약점 보완 모드의 단일 관점 재평가를 JSON-10 형식으로 반환
    @Override
    public WeaknessAnswerEvaluationResult evaluateWeaknessAnswer(String prompt) {
        return WeaknessAnswerEvaluationResult.builder()
                .targetWeaknessTag("SPECIFICITY_WEAK")
                .targetDimension("specificity")
                .currentFollowUpDepth(1)
                .score(78)
                .passThreshold(70)
                .comment("행동 과정은 구체화됐으나 결과 확인 방식이 부족함")
                .passed(true)
                .followUp(WeaknessAnswerEvaluationResult.FollowUp.builder()
                        .depth(2)
                        .question("그 행동의 결과를 어떤 기준이나 지표로 확인했는지 설명해주세요.")
                        .type(FollowUpQuestionType.RESULT_CHECK)
                        .reason("1차 꼬리답변 이후에도 결과 근거 확인이 필요함")
                        .build())
                .build();
    }

    @Override
    public FinalReportResult finalReport(String prompt) {
        return FinalReportResult.builder()
                .interviewMode(InterviewSessionMode.COMPANY_FIT)
                .totalQuestionCount(10)
                .submittedQuestionCount(4)
                .evaluatedQuestionCount(3)
                .evaluationFailedQuestionCount(1)
                .skippedQuestionCount(6)
                .completionRate(40)
                .overallScore(78)
                .scoreLabel("세션 종합 기준 충족도")
                .categoryScores(
                        FinalReportResult.CategoryScores.builder()
                                .intentMatch(81)
                                .specificity(74)
                                .ownRole(85)
                                .problemSolving(68)
                                .resultExpression(62)
                                .requirementConnection(75)
                                .guideAlignment(72)
                                .deliveryClarity(82)
                                .build())
                .basisSummary(
                        FinalReportResult.BasisSummary.builder()
                                .jobCategory("IT·개발 / 백엔드")
                                .careerLevel(JobCategoryCareerLevel.NEW)
                                .evaluationPassThreshold(70)
                                .usedGuide(FinalReportResult.UsedGuide.builder()
                                        .guideId(12L)
                                        .version("v1.2")
                                        .build()
                                )
                                .requirementConnections(List.of(
                                        FinalReportResult.RequirementConnections.builder()
                                                .requirement("Spring Boot 기반 백엔드 개발 경험")
                                                .matchLevel(MatchAnalysisResultMatchLevel.HIGH)
                                                .build()
                                ))
                                .missingEvidence(List.of("성과 수치 근거 부족"))
                                .targetWeaknessTag(null)
                                .targetDimension(null)
                                .originEvaluationIds(List.of())
                                .build())
                .weaknessTagSummary(List.of(
                        FinalReportResult.WeaknessTagSummary.builder()
                                .tag("RESULT_EXPRESSION_WEAK")
                                .count(2).build()))
                .nextPracticeRecommendation(List.of(
                        FinalReportResult.NextPracticeRecommendation.builder()
                                .questionType(InterviewQuestionType.PROBLEM_SOLVING)
                                .reason("해결 과정 설명 보완 필요").build()))
                .improvementSuggestion(
                        FinalReportResult.ImprovementSuggestion.builder()
                                .resume(List.of("담당 기능과 결과를 구체적으로 추가"))
                                .coverLetter(List.of("직무 연결성을 보완"))
                                .portfolio(List.of("문제 해결 과정을 별도 정리"))
                                .experienceNote(List.of("성과를 포함해 STAR로 재정리"))
                                .build())
                .learningDirection(List.of("AWS 배포 기초 학습"))
                .build();
    }

    // 현재 AI 클라이언트 구현체가 사용하는 제공자를 반환
    @Override
    public AiProvider getProvider() {
        return AiProvider.MOCK;
    }

    @Override
    public String call(String prompt) {
        // TODO: Mock JSON 응답 반환
        return "{}";
    }
}
