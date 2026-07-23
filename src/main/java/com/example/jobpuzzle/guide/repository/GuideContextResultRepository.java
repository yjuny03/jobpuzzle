package com.example.jobpuzzle.guide.repository;

import com.example.jobpuzzle.guide.entity.GuideContextInputReferenceType;
import com.example.jobpuzzle.guide.entity.GuideContextPurpose;
import com.example.jobpuzzle.guide.entity.GuideContextResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuideContextResultRepository extends JpaRepository<GuideContextResult, Long> {
    Optional<GuideContextResult> findByPurposeAndInputReferenceTypeAndInputReferenceId(
            GuideContextPurpose purpose, GuideContextInputReferenceType inputReferenceType, String inputReferenceId);

    void deleteByUser_UserId(Long userId);
}
