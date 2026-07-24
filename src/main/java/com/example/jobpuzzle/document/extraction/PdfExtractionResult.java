package com.example.jobpuzzle.document.extraction;

import java.util.List;

public record PdfExtractionResult(
        int pageCount,
        List<PdfPageResult> pages
) {
}