package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.dto.BasicQuestionGenerationInput;
import com.example.jobpuzzle.ai.dto.WeaknessQuestionGenerationInput;
import com.example.jobpuzzle.evaluation.entity.AnswerEvaluation;
import com.example.jobpuzzle.evaluation.entity.WeaknessTagLog;
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
        return new WeaknessQuestionGenerationInput.OriginEvaluation(
                evaluation.getEvaluationId(),
                evaluation.getSessionQuestion().getQuestionTextSnapshot(),
                evaluation.getAnswerMessage().getMessageText(),
                evaluation.getScore(),
                evaluation.getEvaluationDetail(),
                evaluation.getSummary()
        );
    }
}
