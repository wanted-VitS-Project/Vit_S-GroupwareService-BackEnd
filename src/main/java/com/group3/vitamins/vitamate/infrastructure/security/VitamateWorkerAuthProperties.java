package com.group3.vitamins.vitamate.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Python worker 내부 API 인증에 사용하는 설정값
@ConfigurationProperties(prefix = "vitamate.worker")
public record VitamateWorkerAuthProperties(
        String token
) {
}