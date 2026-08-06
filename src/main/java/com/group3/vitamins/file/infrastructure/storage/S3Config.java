package com.group3.vitamins.file.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 클라이언트·presigner 빈. 자격증명은 AWS SDK 기본 자격증명 체인(로컬 {@code aws configure} ·
 * 배포 IAM Role/환경변수)에서 자동 해석된다 — 코드에 키를 넣지 않는다(PUBLIC 레포).
 *
 * <p>리전은 {@code cloud.aws.region}(기본 ap-northeast-2), 버킷은 {@code cloud.aws.s3.bucket} 을 쓴다.
 */
@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${cloud.aws.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(@Value("${cloud.aws.region}") String region) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
