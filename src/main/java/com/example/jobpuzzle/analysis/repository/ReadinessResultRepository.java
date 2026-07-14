package com.example.jobpuzzle.analysis.repository;

import com.example.jobpuzzle.analysis.entity.ReadinessResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadinessResultRepository extends JpaRepository<ReadinessResult, Long> {
}
