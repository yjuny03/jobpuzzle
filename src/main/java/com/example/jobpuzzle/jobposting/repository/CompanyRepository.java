package com.example.jobpuzzle.jobposting.repository;

import com.example.jobpuzzle.jobposting.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
