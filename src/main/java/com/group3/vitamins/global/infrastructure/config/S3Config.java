package com.group3.vitamins.global.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3Client/S3Presigner 는 자격증명을 코드에 두지 않는다 — SDK 기본 자격증명 체인(환경변수 · EC2/ECS
 * 인스턴스 역할 · {@code ~/.aws/credentials})을 그대로 쓴다 (`.ai/API.md` §6 — PUBLIC 레포).
 */
@Configuration
public class S3Config {

    @Value("${cloud.aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    /** 버킷이 퍼블릭 액세스 전체 차단 상태라, 프론트에 노출하는 URL은 이걸로 서명해 발급한다. */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
