package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findByQuestionSet_QuestionSetIdOrderByDisplayOrderAsc(Long questionSetId);
    List<InterviewQuestion> findByQuestionSet_QuestionSetIdAndQuestionIdIn(Long questionSetId, Collection<Long> questionIds);
    void deleteByQuestionSet_QuestionSetId(Long questionSetId);
    void deleteByQuestionSet_Snapshot_User_UserId(Long userId);
}
