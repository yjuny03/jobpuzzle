package com.example.jobpuzzle.document.extraction;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.DocumentVersionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.repository.DocumentExtractionRepository;
import com.example.jobpuzzle.document.repository.UserDocumentRepository;
import com.example.jobpuzzle.document.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

// DocumentExtractionService.extractDocumentText가 호출을 위임하는 실제 추출 작업.
// 별도 빈으로 분리한 이유: 같은 클래스 안에서 @Async 메서드를 호출하면 프록시를 안 거쳐서 비동기로 동작하지 않기 때문
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentExtractionAsyncRunner {

    private final UserDocumentRepository userDocumentRepository;
    private final DocumentExtractionRepository documentExtractionRepository;
    private final FileStorage fileStorage;
    private final PdfTextExtractor pdfTextExtractor;
    private final OcrEngine ocrEngine;

    @Async("documentExtractionExecutor")
    public void run(Long documentId) {
        UserDocument document = userDocumentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("추출 대상 자료를 찾을 수 없습니다. documentId={}", documentId);
            return;
        }

        ExtractionOutcome outcome;
        try {
            outcome = extract(document);
        } catch (DocumentExtractionFailedException e) {
            outcome = ExtractionOutcome.failed(e.getMessage());
        } catch (Exception e) {
            log.error("문서 추출 중 예기치 못한 오류. documentId={}", documentId, e);
            outcome = ExtractionOutcome.failed("알 수 없는 오류로 추출에 실패했습니다.");
        }

        saveExtraction(document, outcome);
        applyRetentionPolicy(document.getDocumentId());
    }

    private ExtractionOutcome extract(UserDocument document) {
        try (InputStream inputStream = fileStorage.load(document.getFilePath())) {
            return switch (document.getSourceType()) {
                case PDF -> extractPdf(inputStream);
                case IMAGE -> extractImage(inputStream);
                case TEXT -> throw new IllegalStateException("TEXT 자료는 비동기 추출 대상이 아닙니다.");
            };
        } catch (IOException e) {
            throw new DocumentExtractionFailedException("저장된 파일을 불러오지 못했습니다.", e);
        }
    }

    private ExtractionOutcome extractPdf(InputStream inputStream) {
        PdfExtractionResult result = pdfTextExtractor.extract(inputStream);

        StringBuilder content = new StringBuilder();
        boolean ocrApplied = false;
        int emptyPageCount = 0;

        for (PdfPageResult page : result.pages()) {
            String pageText = page.scanned() ? ocrEngine.recognize(page.renderedImage()) : page.text();
            if (page.scanned()) {
                ocrApplied = true;
            }
            if (pageText == null || pageText.isBlank()) {
                emptyPageCount++;
            } else {
                // 여러 페이지 문서를 편집 화면에서 시각적으로 구분해 보이도록, 그리고 나중에 페이지 단위로 원문 위치를 찾을 수 있도록 마커를 남김
                content.append("[").append(page.pageNumber()).append("페이지]\n");
                content.append(pageText).append("\n\n");
            }
        }

        String text = content.toString().trim();
        if (emptyPageCount == result.pageCount()) {
            return ExtractionOutcome.failed("모든 페이지에서 텍스트를 추출하지 못했습니다.", ocrApplied);
        }
        if (emptyPageCount > 0) {
            return ExtractionOutcome.partial(text, result.pageCount(), ocrApplied,
                    emptyPageCount + "개 페이지에서 텍스트를 추출하지 못했습니다.");
        }
        return ExtractionOutcome.success(text, result.pageCount(), ocrApplied);
    }

    private ExtractionOutcome extractImage(InputStream inputStream) {
        BufferedImage image;
        try {
            image = ImageIO.read(inputStream);
        } catch (IOException e) {
            throw new DocumentExtractionFailedException("이미지 파일을 읽는 중 오류가 발생했습니다.", e);
        }
        if (image == null) {
            throw new DocumentExtractionFailedException("이미지 파일을 읽을 수 없습니다.");
        }

        String text = ocrEngine.recognize(image);
        if (text == null || text.isBlank()) {
            return ExtractionOutcome.failed("이미지에서 텍스트를 인식하지 못했습니다.", true);
        }
        return ExtractionOutcome.success(text, null, true);
    }

    // 추출 직후엔 항상 버전 번호 없이 저장 (첫 확정 시점에 1.0 부여 - DocumentExtraction.confirm() 참고)
    private void saveExtraction(UserDocument document, ExtractionOutcome outcome) {
        boolean completeFailure = outcome.status() == DocumentExtractionStatus.FAILED;

        DocumentExtraction extraction = DocumentExtraction.builder()
                .document(document)
                .extractionStatus(outcome.status())
                .versionStatus(completeFailure ? null : DocumentVersionStatus.DRAFT)
                .content(completeFailure ? null : outcome.text())
                .pageCount(outcome.pageCount())
                .ocrApplied(outcome.ocrApplied())
                .failureReason(outcome.failureReason())
                .build();

        documentExtractionRepository.save(extraction);
    }

    // 추출 진행 중 사용자가 원본 보관 설정을 바꿨을 수 있으므로 완료 시점에 최신 값을 다시 읽어 판단
    private void applyRetentionPolicy(Long documentId) {
        UserDocument document = userDocumentRepository.findById(documentId).orElse(null);
        if (document != null && !document.isKeepOriginal() && document.getFilePath() != null) {
            fileStorage.delete(document.getFilePath());
            document.clearFilePath();
            userDocumentRepository.save(document);
        }
    }

    private record ExtractionOutcome(
            DocumentExtractionStatus status,
            String text,
            Integer pageCount,
            boolean ocrApplied,
            String failureReason
    ) {
        static ExtractionOutcome success(String text, Integer pageCount, boolean ocrApplied) {
            return new ExtractionOutcome(DocumentExtractionStatus.SUCCESS, text, pageCount, ocrApplied, null);
        }

        static ExtractionOutcome partial(String text, Integer pageCount, boolean ocrApplied, String reason) {
            return new ExtractionOutcome(DocumentExtractionStatus.PARTIAL, text, pageCount, ocrApplied, reason);
        }

        static ExtractionOutcome failed(String reason) {
            return new ExtractionOutcome(DocumentExtractionStatus.FAILED, null, null, false, reason);
        }

        static ExtractionOutcome failed(String reason, boolean ocrApplied) {
            return new ExtractionOutcome(DocumentExtractionStatus.FAILED, null, null, ocrApplied, reason);
        }
    }
}