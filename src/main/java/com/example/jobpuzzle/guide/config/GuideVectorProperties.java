package com.example.jobpuzzle.guide.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 가이드 전용 벡터 collection과 검색 크기를 분석 자료 RAG 설정과 분리한다. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.guide.vector")
public class GuideVectorProperties {
    private String collection = "job-guide-chunks-te3small-v1";
    private int topK = 8;
}
