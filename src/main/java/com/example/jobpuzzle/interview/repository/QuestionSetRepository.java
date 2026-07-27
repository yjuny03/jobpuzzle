package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewSessionMode;
import com.example.jobpuzzle.interview.entity.QuestionSet;
import com.example.jobpuzzle.interview.entity.QuestionSetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, Long> {
    Optional<QuestionSet> findByQuestionSetIdAndUser_UserId(Long questionSetId, Long userId);
    boolean existsByUser_UserIdAndInterviewModeAndStatus(
            Long userId,
            InterviewSessionMode interviewMode,
            QuestionSetStatus status
    );
    Optional<QuestionSet> findBySnapshot_SnapshotIdAndInterviewMode(Long snapshotId, InterviewSessionMode interviewMode);
    boolean existsBySnapshot_SnapshotIdAndInterviewMode(Long snapshotId, InterviewSessionMode interviewMode);
    void deleteBySnapshot_SnapshotId(Long snapshotId);
    void deleteBySnapshot_User_UserId(Long userId);
}
