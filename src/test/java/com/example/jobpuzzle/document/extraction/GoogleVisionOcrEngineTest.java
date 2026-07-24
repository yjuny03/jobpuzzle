package com.example.jobpuzzle.document.extraction;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

// 서비스 계정 인증 파일이 없으면 실제 API 호출 없이 스킵
class GoogleVisionOcrEngineTest {

    private static final String CREDENTIALS_PATH = "./credentials/google-vision-service-account.json";

    private GoogleVisionOcrEngine ocrEngine;

    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(
                Files.exists(Path.of(CREDENTIALS_PATH)),
                CREDENTIALS_PATH + " 파일이 없어 스킵함"
        );
        ocrEngine = new GoogleVisionOcrEngine(CREDENTIALS_PATH);
    }

    @Test
    @DisplayName("한글 텍스트 이미지에서 텍스트를 인식한다")
    void recognizesTextFromKoreanImage() {
        BufferedImage image = OcrTestImages.createTextImage("테스트 문서입니다");

        String result = ocrEngine.recognize(image);

        assertThat(result.replaceAll("\\s+", "")).contains("테스트문서입니다");
    }

    @Test
    @DisplayName("한글+영어 혼합 문단 이미지에서 기술 용어까지 정확히 인식한다")
    void recognizesTechTermsInMixedKoreanEnglishParagraph() {
        String[] lines = {
                "백엔드 개발 프로젝트에서 Spring Boot와 JPA를 활용하여 회원 API를 구현했습니다.",
                "인증 로직과 예외 처리를 개선하여 시스템 안정성을 높였습니다.",
                "데이터베이스 설계와 쿼리 최적화를 통해 응답 속도를 개선했습니다."
        };
        BufferedImage image = OcrTestImages.createParagraphImage(lines);

        String result = ocrEngine.recognize(image);

        assertThat(result).contains("Spring Boot", "JPA", "API", "데이터베이스", "쿼리 최적화");
    }
}