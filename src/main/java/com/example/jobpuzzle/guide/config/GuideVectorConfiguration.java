package com.example.jobpuzzle.guide.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 가이드 전용 Qdrant 검색 설정을 등록한다. 임베딩 공급자는 기존 RAG 설정을 함께 사용한다. */
@Configuration
@EnableConfigurationProperties(GuideVectorProperties.class)
public class GuideVectorConfiguration {
}
