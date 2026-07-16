package com.example.jobpuzzle.analysis.entity;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult.*;
import com.example.jobpuzzle.ai.log.AiCallLog;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.global.common.BaseEntity;
import com.example.jobpuzzle.jobcategory.entity.JobCategory;
import com.example.jobpuzzle.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "candidate_material_analysis")
public class CandidateMaterialAnalysis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long analysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id", nullable = false)
    private JobCategory jobCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_document_id")
    private UserDocument resumeDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_letter_document_id")
    private UserDocument coverLetterDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_document_id")
    private UserDocument portfolioDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_note_document_id")
    private UserDocument experienceNoteDocument;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resume_analysis",columnDefinition = "json")
    private Resume resumeAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cover_letter_analysis",columnDefinition = "json")
    private CoverLetter coverLetterAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "portfolio_analysis",columnDefinition = "json")
    private Portfolio portfolioAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "experience_note_analysis",columnDefinition = "json")
    private ExperienceNote experienceNoteAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_evidence",columnDefinition = "json")
    private List<String> missingEvidence;

    @Column(name= "is_edited",nullable = false)
    private boolean isEdited = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_call_log_id")
    private AiCallLog aiCallLog;

    @Builder
    private CandidateMaterialAnalysis(
            User user,
            JobCategory jobCategory,
            UserDocument resumeDocument,
            UserDocument coverLetterDocument,
            UserDocument portfolioDocument,
            UserDocument experienceNoteDocument,
            Resume resumeAnalysis,
            CoverLetter coverLetterAnalysis,
            Portfolio portfolioAnalysis,
            ExperienceNote experienceNoteAnalysis,
            List<String> missingEvidence,
            AiCallLog aiCallLog
    ){
        this.user = user;
        this.jobCategory = jobCategory;
        this.resumeDocument = resumeDocument;
        this.coverLetterDocument = coverLetterDocument;
        this.portfolioDocument = portfolioDocument;
        this.experienceNoteDocument = experienceNoteDocument;
        this.resumeAnalysis = resumeAnalysis;
        this.coverLetterAnalysis = coverLetterAnalysis;
        this.portfolioAnalysis = portfolioAnalysis;
        this.experienceNoteAnalysis = experienceNoteAnalysis;
        this.missingEvidence = missingEvidence;
        this.isEdited = false;
        this.aiCallLog = aiCallLog;
    }

}
