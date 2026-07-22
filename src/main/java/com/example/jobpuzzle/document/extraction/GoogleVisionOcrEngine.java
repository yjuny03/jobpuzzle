package com.example.jobpuzzle.document.extraction;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Component
public class GoogleVisionOcrEngine implements OcrEngine {

    private static final String ENDPOINT = "https://vision.googleapis.com/v1/images:annotate";
    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    private final RestClient restClient;
    private final GoogleCredentials credentials;

    // 서비스 계정 JSON에서 자격증명을 로드해 cloud-platform 스코프로 고정
    public GoogleVisionOcrEngine(@Value("${app.ocr.google-vision.credentials-path}") String credentialsPath) {
        this.restClient = RestClient.create();
        try (FileInputStream inputStream = new FileInputStream(credentialsPath)) {
            this.credentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(List.of(SCOPE));
        } catch (IOException e) {
            throw new IllegalStateException("Google Vision 서비스 계정 인증 파일을 읽을 수 없습니다: " + credentialsPath, e);
        }
    }

    // 이미지를 base64로 인코딩해 Vision API images:annotate(DOCUMENT_TEXT_DETECTION)를 호출하고 전체 인식 텍스트를 반환
    @Override
    public String recognize(BufferedImage image) {
        String base64Image = encodeToBase64(image);

        VisionRequest request = new VisionRequest(List.of(
                new AnnotateImageRequest(
                        new Image(base64Image),
                        List.of(new Feature("DOCUMENT_TEXT_DETECTION")),
                        new ImageContext(List.of("ko", "en"))
                )
        ));

        VisionResponse response;
        try {
            response = restClient.post()
                    .uri(ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                    .body(request)
                    .retrieve()
                    .body(VisionResponse.class);
        } catch (RestClientException e) {
            throw new DocumentExtractionFailedException("Google Vision OCR 요청에 실패했습니다.", e);
        }

        if (response == null || response.responses().isEmpty()) {
            throw new DocumentExtractionFailedException("Google Vision OCR 응답이 비어 있습니다.");
        }

        AnnotateImageResponse result = response.responses().get(0);
        if (result.error() != null) {
            throw new DocumentExtractionFailedException("Google Vision OCR 오류: " + result.error().message());
        }
        if (result.fullTextAnnotation() == null) {
            return "";
        }
        return result.fullTextAnnotation().text().trim();
    }

    // 만료됐을 때만 실제로 갱신하므로 매 호출마다 불러도 됨
    private String getAccessToken() {
        try {
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new DocumentExtractionFailedException("Google Vision 인증 토큰을 가져오지 못했습니다.", e);
        }
    }

    // Vision API 요청 바디(image.content)에 넣을 base64 PNG로 변환
    private String encodeToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            return Base64.getEncoder().encodeToString(buffer.toByteArray());
        } catch (IOException e) {
            throw new DocumentExtractionFailedException("이미지를 인코딩하는 중 오류가 발생했습니다.", e);
        }
    }

    // 아래는 Vision API 요청/응답 JSON 구조를 그대로 옮긴 record들
    private record VisionRequest(List<AnnotateImageRequest> requests) {
    }

    private record AnnotateImageRequest(Image image, List<Feature> features, ImageContext imageContext) {
    }

    private record Image(String content) {
    }

    private record Feature(String type) {
    }

    private record ImageContext(List<String> languageHints) {
    }

    private record VisionResponse(List<AnnotateImageResponse> responses) {
    }

    private record AnnotateImageResponse(FullTextAnnotation fullTextAnnotation, VisionError error) {
    }

    private record FullTextAnnotation(String text) {
    }

    private record VisionError(int code, String message) {
    }
}