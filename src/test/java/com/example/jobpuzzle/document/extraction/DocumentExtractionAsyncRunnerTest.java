package com.example.jobpuzzle.document.extraction;

import com.example.jobpuzzle.document.entity.DocumentExtraction;
import com.example.jobpuzzle.document.entity.DocumentExtractionStatus;
import com.example.jobpuzzle.document.entity.UserDocument;
import com.example.jobpuzzle.document.entity.UserDocumentSourceType;
import com.example.jobpuzzle.document.entity.UserDocumentType;
import com.example.jobpuzzle.document.repository.DocumentExtractionRepository;
import com.example.jobpuzzle.document.repository.UserDocumentRepository;
import com.example.jobpuzzle.document.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExtractionAsyncRunnerTest {

    @Mock
    private UserDocumentRepository userDocumentRepository;
    @Mock
    private DocumentExtractionRepository documentExtractionRepository;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private PdfTextExtractor pdfTextExtractor;
    @Mock
    private OcrEngine ocrEngine;

    @InjectMocks
    private DocumentExtractionAsyncRunner runner;

    @Test
    @DisplayName("이미지 여러 장을 OCR로 읽어 [N페이지] 마커로 이어붙인다")
    void extractImage_multipleImages_success() throws IOException {
        UserDocument document = imageDocument(true, "cap1.png", "cap2.png");
        when(userDocumentRepository.findWithFilesById(1L)).thenReturn(Optional.of(document));
        when(fileStorage.load(anyString())).thenReturn(pngStream(), pngStream());
        when(ocrEngine.recognize(any())).thenReturn("첫번째 이미지 텍스트").thenReturn("두번째 이미지 텍스트");

        runner.run(1L);

        DocumentExtraction saved = captureSavedExtraction();
        assertThat(saved.getExtractionStatus()).isEqualTo(DocumentExtractionStatus.SUCCESS);
        assertThat(saved.getPageCount()).isEqualTo(2);
        assertThat(saved.getContent()).isEqualTo(
                "[1페이지]\n첫번째 이미지 텍스트\n\n[2페이지]\n두번째 이미지 텍스트"
        );
    }

    @Test
    @DisplayName("일부 이미지에서만 텍스트가 인식되면 PARTIAL로 저장된다")
    void extractImage_someImagesEmpty_partial() throws IOException {
        UserDocument document = imageDocument(true, "cap1.png", "cap2.png");
        when(userDocumentRepository.findWithFilesById(1L)).thenReturn(Optional.of(document));
        when(fileStorage.load(anyString())).thenReturn(pngStream(), pngStream());
        when(ocrEngine.recognize(any())).thenReturn("").thenReturn("두번째 이미지 텍스트");

        runner.run(1L);

        DocumentExtraction saved = captureSavedExtraction();
        assertThat(saved.getExtractionStatus()).isEqualTo(DocumentExtractionStatus.PARTIAL);
        assertThat(saved.getPageCount()).isEqualTo(2);
        assertThat(saved.getContent()).isEqualTo("[2페이지]\n두번째 이미지 텍스트");
        assertThat(saved.getFailureReason()).isEqualTo("1개 이미지에서 텍스트를 추출하지 못했습니다.");
    }

    @Test
    @DisplayName("모든 이미지에서 텍스트를 인식하지 못하면 FAILED로 저장된다")
    void extractImage_allImagesEmpty_failed() throws IOException {
        UserDocument document = imageDocument(true, "cap1.png");
        when(userDocumentRepository.findWithFilesById(1L)).thenReturn(Optional.of(document));
        when(fileStorage.load(anyString())).thenReturn(pngStream());
        when(ocrEngine.recognize(any())).thenReturn(null);

        runner.run(1L);

        DocumentExtraction saved = captureSavedExtraction();
        assertThat(saved.getExtractionStatus()).isEqualTo(DocumentExtractionStatus.FAILED);
        assertThat(saved.getContent()).isNull();
    }

    @Test
    @DisplayName("추출 완료 후 원본 보관을 꺼두면 자료에 딸린 파일이 전부 삭제된다")
    void applyRetentionPolicy_deletesAllFiles_whenKeepOriginalFalse() throws IOException {
        UserDocument document = imageDocument(false, "cap1.png", "cap2.png");
        when(userDocumentRepository.findWithFilesById(1L)).thenReturn(Optional.of(document));
        when(fileStorage.load(anyString())).thenReturn(pngStream(), pngStream());
        when(ocrEngine.recognize(any())).thenReturn("텍스트");

        runner.run(1L);

        verify(fileStorage, times(2)).delete(anyString());
        assertThat(document.getFiles()).isEmpty();
    }

    @Test
    @DisplayName("원본 보관을 켜두면 파일이 삭제되지 않는다")
    void applyRetentionPolicy_keepsFiles_whenKeepOriginalTrue() throws IOException {
        UserDocument document = imageDocument(true, "cap1.png");
        when(userDocumentRepository.findWithFilesById(1L)).thenReturn(Optional.of(document));
        when(fileStorage.load(anyString())).thenReturn(pngStream());
        when(ocrEngine.recognize(any())).thenReturn("텍스트");

        runner.run(1L);

        verify(fileStorage, never()).delete(anyString());
        assertThat(document.getFiles()).hasSize(1);
    }

    private DocumentExtraction captureSavedExtraction() {
        ArgumentCaptor<DocumentExtraction> captor = ArgumentCaptor.forClass(DocumentExtraction.class);
        verify(documentExtractionRepository).save(captor.capture());
        return captor.getValue();
    }

    private UserDocument imageDocument(boolean keepOriginal, String... fileNames) {
        UserDocument document = UserDocument.builder()
                .documentType(UserDocumentType.JOB_POSTING).sourceType(UserDocumentSourceType.IMAGE)
                .displayName("스크린샷 공고").keepOriginal(keepOriginal).build();
        for (String fileName : fileNames) {
            document.addFile("image/" + fileName, fileName);
        }
        ReflectionTestUtils.setField(document, "documentId", 1L);
        return document;
    }

    private InputStream pngStream() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new ByteArrayInputStream(out.toByteArray());
    }
}