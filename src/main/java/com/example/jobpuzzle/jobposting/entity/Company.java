package com.example.jobpuzzle.jobposting.entity;

import com.example.jobpuzzle.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "company")
public class Company extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name= "company_id")
    private Long companyId;

    @Column(name = "company_name",nullable = false, length = 200)
    private String companyName;

    @Column(name = "industry",length = 100)
    private String industry;

    @Builder
    private Company(
            String companyName,
            String industry
    ) {
        this.companyName = companyName;
        this.industry = industry;
    }

    // 회사명과 산업군 정보를 수정
    public void updateCompanyInfo(String companyName, String industry) {
        this.companyName = companyName;
        this.industry = industry;
    }
}
