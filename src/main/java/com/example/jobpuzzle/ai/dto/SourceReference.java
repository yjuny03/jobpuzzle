package com.example.jobpuzzle.ai.dto;

import com.example.jobpuzzle.document.entity.UserDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// JSON-01·02가 공통으로 사용하는 원문 근거 위치 계약이다.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceReference {

    private Long extractionId;
    private Long documentId;
    private UserDocumentType documentType;
    private Integer pageNumber;
    private String segmentId;
    private String evidenceText;
}
