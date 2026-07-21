package com.example.jobpuzzle.document.extraction;

import java.awt.image.BufferedImage;

// scanned=true면 텍스트 밀도가 낮아 OCR 대상으로 판단
// renderedImage에 OCR용 렌더링 이미지가 채워짐
public record PdfPageResult(
        int pageNumber,
        String text,
        boolean scanned,
        BufferedImage renderedImage
) {
}