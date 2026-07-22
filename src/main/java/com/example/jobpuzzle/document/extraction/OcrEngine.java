package com.example.jobpuzzle.document.extraction;

import java.awt.image.BufferedImage;

// 나중에 다른 OCR 엔진(클라우드 API 등)으로 교체할 수 있도록 구현을 인터페이스 뒤로 숨김
public interface OcrEngine {

    String recognize(BufferedImage image);
}