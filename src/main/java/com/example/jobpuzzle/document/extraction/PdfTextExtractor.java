package com.example.jobpuzzle.document.extraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfTextExtractor {

    // 이 글자 수 미만이면 텍스트형이 아닌 스캔형(이미지 기반) 페이지로 판단해 OCR 대상으로 넘김
    private static final int MIN_TEXT_LENGTH_PER_PAGE = 10;
    private static final float RENDER_DPI = 300f;

    public PdfExtractionResult extract(InputStream pdfInputStream) {
        byte[] bytes = readBytes(pdfInputStream);

        try (PDDocument document = Loader.loadPDF(bytes)) {
            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new DocumentExtractionFailedException("페이지가 없는 PDF 파일입니다.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            PDFRenderer renderer = new PDFRenderer(document);
            List<PdfPageResult> pages = new ArrayList<>();

            for (int pageIndex = 1; pageIndex <= pageCount; pageIndex++) {
                pages.add(extractPage(stripper, renderer, document, pageIndex));
            }

            return new PdfExtractionResult(pageCount, pages);
        } catch (InvalidPasswordException e) {
            throw new DocumentExtractionFailedException("비밀번호로 보호된 PDF는 지원하지 않습니다.", e);
        } catch (IOException e) {
            throw new DocumentExtractionFailedException("PDF 파일을 열 수 없습니다. 파일이 손상되었거나 지원하지 않는 형식입니다.", e);
        }
    }

    private PdfPageResult extractPage(
            PDFTextStripper stripper,
            PDFRenderer renderer,
            PDDocument document,
            int pageIndex
    ) {
        try {
            stripper.setStartPage(pageIndex);
            stripper.setEndPage(pageIndex);
            String text = stripper.getText(document).trim();
            boolean scanned = text.length() < MIN_TEXT_LENGTH_PER_PAGE;

            BufferedImage renderedImage = scanned
                    ? renderer.renderImageWithDPI(pageIndex - 1, RENDER_DPI)
                    : null;

            return new PdfPageResult(pageIndex, text, scanned, renderedImage);
        } catch (IOException e) {
            throw new DocumentExtractionFailedException(
                    pageIndex + "페이지를 처리하는 중 오류가 발생했습니다.", e
            );
        }
    }

    private byte[] readBytes(InputStream inputStream) {
        try {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new DocumentExtractionFailedException("파일을 읽는 중 오류가 발생했습니다.", e);
        }
    }
}