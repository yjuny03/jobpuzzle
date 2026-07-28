package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.dto.BasicQuestionGenerationInput;
import com.example.jobpuzzle.ai.dto.WeaknessQuestionGenerationInput;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagLog;
import com.example.jobpuzzle.interview.entity.InterviewMessage;
import com.example.jobpuzzle.interview.entity.InterviewSessionQuestion;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 질문 생성용 DB Entity를 AI 입력 전용 DTO로 변환한다.
 */
@Component
public class InterviewQuestionGenerationInputMapper {

    public BasicQuestionGenerationInput basic(JobCategory jobCategory) {
        return new BasicQuestionGenerationInput(
                jobCategory.getJobCategoryId(),
                jobCategory.getMainCategory(),
                jobCategory.getSubCategory(),
                jobCategory.getCareerLevel()
        );
    }

    public WeaknessQuestionGenerationInput weakness(
            String targetWeaknessTag,
            String targetDimension,
            List<WeaknessTagLog> origins
    ) {
        return new WeaknessQuestionGenerationInput(
                targetWeaknessTag,
                targetDimension,
                origins.stream().map(this::origin).toList()
        );
    }

    private WeaknessQuestionGenerationInput.OriginEvaluation origin(WeaknessTagLog log) {
        AnswerEvaluation evaluation = log.getEvaluation();
        InterviewSessionQuestion sessionQuestion = evaluation.getSessionQuestion();
        InterviewMessage answerMessage = evaluation.getAnswerMessage();
        String question = sessionQuestion == null
                ? "이전 면접 질문 정보 없음"
                : sessionQuestion.getQuestionTextSnapshot();
        String answer = answerMessage == null
                ? evaluation.getSummary()
                : answerMessage.getMessageText();
        return new WeaknessQuestionGenerationInput.OriginEvaluation(
                evaluation.getEvaluationId(),
                question == null || question.isBlank() ? "이전 면접 질문 정보 없음" : question,
                answer == null || answer.isBlank() ? "이전 답변 상세 정보 없음" : answer,
                evaluation.getScore(),
                evaluation.getEvaluationDetail(),
                evaluation.getSummary()
        );
    }
}
