package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// 나라장터 외부 API 호출 설정을 관리합니다.
@ConfigurationProperties(prefix = "bidding.nara")
public record NaraBidNoticeClientProperties(
        String baseUrl,
        String serviceKey,
        int pageSize,
        Duration connectTimeout,
        Duration readTimeout
) {

    public NaraBidNoticeClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("나라장터 API base-url은 필수입니다.");
        }
        if (pageSize < 1 || pageSize > 999) {
            throw new IllegalArgumentException("나라장터 API page-size는 1~999여야 합니다.");
        }
        if (connectTimeout == null
                || connectTimeout.isNegative()
                || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connect-timeout은 0보다 커야 합니다.");
        }

        if (readTimeout == null
                || readTimeout.isNegative()
                || readTimeout.isZero()) {
            throw new IllegalArgumentException("read-timeout은 0보다 커야 합니다.");
        }
    }
}