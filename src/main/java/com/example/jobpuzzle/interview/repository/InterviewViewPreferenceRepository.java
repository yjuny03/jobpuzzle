package com.example.jobpuzzle.interview.repository;

import com.example.jobpuzzle.interview.entity.InterviewViewPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterviewViewPreferenceRepository extends JpaRepository<InterviewViewPreference, Long> {
    Optional<InterviewViewPreference> findByUser_UserId(Long userId);
}
