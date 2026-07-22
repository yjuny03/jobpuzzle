package com.example.jobpuzzle.document.extraction;

// 텍스트/OCR 추출 실패
// API 응답 예외(CustomException)와 달리 추출 파이프라인 내부에서만 사용
public class DocumentExtractionFailedException extends RuntimeException {

    public DocumentExtractionFailedException(String reason) {
        super(reason);
    }

    public DocumentExtractionFailedException(String reason, Throwable cause) {
        super(reason, cause);
    }
}