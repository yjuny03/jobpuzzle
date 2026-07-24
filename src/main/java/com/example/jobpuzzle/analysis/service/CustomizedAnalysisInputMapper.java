package com.example.jobpuzzle.analysis.service;

import com.example.jobpuzzle.ai.dto.CandidateMaterialAnalysisResult;
import com.example.jobpuzzle.ai.dto.JobPostingAnalysisResult;
import com.example.jobpuzzle.ai.dto.SourceReference;
import com.example.jobpuzzle.analysis.entity.AnalysisSourceReference;
import com.example.jobpuzzle.analysis.entity.CandidateMaterialAnalysis;
import com.example.jobpuzzle.analysis.entity.JobPostingAnalysis;
import org.springframework.stereotype.Component;

import java.util.List;

// 저장된 JSON-01·02 값을 JSON-05 Provider/validator 입력 DTO로 명시적으로 복원한다.
@Component
public class CustomizedAnalysisInputMapper {

    public JobPostingAnalysisResult jobPosting(JobPostingAnalysis value) {
        return JobPostingAnalysisResult.builder()
                .mainTasks(value.getMainTasks().stream().map(this::item).toList())
                .requirements(value.getRequirements().stream().map(this::requirement).toList())
                .preferred(value.getPreferred().stream().map(this::requirement).toList())
                .companyValues(value.getCompanyValues().stream().map(this::item).toList())
                .coreCompetencies(value.getCoreCompetencies().stream().map(this::item).toList())
                .conflicts(value.getConflicts().stream().map(this::conflict).toList())
                .missingEvidence(value.getMissingEvidence().stream().map(item -> JobPostingAnalysisResult.MissingEvidence.builder().item(item.getItem()).reason(item.getReason()).build()).toList())
                .build();
    }

    public CandidateMaterialAnalysisResult candidate(CandidateMaterialAnalysis value) {
        return CandidateMaterialAnalysisResult.builder()
                .availableDocumentTypes(value.getAvailableDocumentTypes())
                .resume(resume(value.getResumeAnalysis())).coverLetter(coverLetter(value.getCoverLetterAnalysis()))
                .portfolio(portfolio(value.getPortfolioAnalysis())).experienceNote(note(value.getExperienceNoteAnalysis()))
                .missingEvidence(value.getMissingEvidence().stream().map(item -> CandidateMaterialAnalysisResult.MissingEvidence.builder().item(item.getItem()).reason(item.getReason()).build()).toList())
                .build();
    }

    private JobPostingAnalysisResult.Item item(JobPostingAnalysis.Item value) { return JobPostingAnalysisResult.Item.builder().itemId(value.getItemId()).text(value.getText()).sourceRefs(refs(value.getSourceRefs())).build(); }
    private JobPostingAnalysisResult.Requirement requirement(JobPostingAnalysis.Requirement value) { return JobPostingAnalysisResult.Requirement.builder().requirementId(value.getRequirementId()).text(value.getText()).sourceRefs(refs(value.getSourceRefs())).build(); }
    private JobPostingAnalysisResult.Conflict conflict(JobPostingAnalysis.Conflict value) { return JobPostingAnalysisResult.Conflict.builder().field(value.getField()).postingValue(value.getPostingValue()).companyInfoValue(value.getCompanyInfoValue()).appliedValue(value.getAppliedValue()).sourceRefs(refs(value.getSourceRefs())).build(); }

    private CandidateMaterialAnalysisResult.Resume resume(CandidateMaterialAnalysis.Resume value) {
        if (value == null) return null;
        return CandidateMaterialAnalysisResult.Resume.builder()
                .experiences(value.getExperiences().stream().map(item -> CandidateMaterialAnalysisResult.Experience.builder().experienceId(item.getExperienceId()).title(item.getTitle()).period(item.getPeriod()).summary(item.getSummary()).sourceRefs(refs(item.getSourceRefs())).build()).toList())
                .skills(value.getSkills().stream().map(item -> CandidateMaterialAnalysisResult.Skill.builder().skill(item.getSkill()).usageContext(item.getUsageContext()).sourceRefs(refs(item.getSourceRefs())).build()).toList())
                .roles(value.getRoles().stream().map(item -> CandidateMaterialAnalysisResult.Role.builder().role(item.getRole()).context(item.getContext()).sourceRefs(refs(item.getSourceRefs())).build()).toList())
                .results(value.getResults().stream().map(item -> CandidateMaterialAnalysisResult.Result.builder().result(item.getResult()).sourceRefs(refs(item.getSourceRefs())).build()).toList()).build();
    }

    private CandidateMaterialAnalysisResult.CoverLetter coverLetter(CandidateMaterialAnalysis.CoverLetter value) {
        if (value == null) return null;
        return CandidateMaterialAnalysisResult.CoverLetter.builder().motivation(summary(value.getMotivation())).values(summary(value.getValues()))
                .experienceNarratives(value.getExperienceNarratives().stream().map(this::summary).toList()).jobConnection(summary(value.getJobConnection())).build();
    }
    private CandidateMaterialAnalysisResult.SummaryEvidence summary(CandidateMaterialAnalysis.SummaryEvidence value) { return value == null ? null : CandidateMaterialAnalysisResult.SummaryEvidence.builder().summary(value.getSummary()).sourceRefs(refs(value.getSourceRefs())).build(); }
    private CandidateMaterialAnalysisResult.Portfolio portfolio(CandidateMaterialAnalysis.Portfolio value) { if (value == null) return null; return CandidateMaterialAnalysisResult.Portfolio.builder().projects(value.getProjects().stream().map(item -> CandidateMaterialAnalysisResult.Project.builder().projectId(item.getProjectId()).projectName(item.getProjectName()).structure(item.getStructure()).role(item.getRole()).contributions(item.getContributions()).techUsageReasons(item.getTechUsageReasons()).problemSolving(item.getProblemSolving()).outputs(item.getOutputs()).sourceRefs(refs(item.getSourceRefs())).build()).toList()).build(); }
    private CandidateMaterialAnalysisResult.ExperienceNote note(CandidateMaterialAnalysis.ExperienceNote value) { if (value == null) return null; return CandidateMaterialAnalysisResult.ExperienceNote.builder().starCandidates(value.getStarCandidates().stream().map(item -> CandidateMaterialAnalysisResult.StarCandidate.builder().candidateId(item.getCandidateId()).situation(item.getSituation()).task(item.getTask()).action(item.getAction()).result(item.getResult()).missingParts(item.getMissingParts()).sourceRefs(refs(item.getSourceRefs())).build()).toList()).build(); }
    private List<SourceReference> refs(List<AnalysisSourceReference> values) { return values == null ? List.of() : values.stream().map(value -> SourceReference.builder().extractionId(value.getExtractionId()).documentId(value.getDocumentId()).documentType(value.getDocumentType()).pageNumber(value.getPageNumber()).segmentId(value.getSegmentId()).evidenceText(value.getEvidenceText()).build()).toList(); }
}
