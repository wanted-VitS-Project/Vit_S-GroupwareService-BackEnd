package com.group3.vitamins.file.application.port;

import java.time.Instant;
import java.util.Optional;

/**
 * 파일 저장소(S3) 아웃바운드 포트. 애플리케이션은 S3 SDK 를 직접 모른다 — 구현은
 * {@code infrastructure/storage/S3FileStorageAdapter}.
 *
 * <p>업로드·다운로드는 presigned URL 로 클라이언트가 저장소와 직접 주고받고, 서버는 URL 발급과
 * 완료 검증(HEAD)만 한다. 미리보기용 {@code getObject}(서버가 앞 5p 잘라 반환)는 §10 구현 시 추가한다.
 */
public interface FileStoragePort {

    /** 업로드용 presigned PUT URL 을 발급한다(만료 10분). */
    PresignedUrl presignUpload(String storageKey, String contentType, long sizeBytes);

    /** 다운로드용 presigned GET URL 을 발급한다(만료 5분, 원본 파일명으로 attachment). */
    PresignedUrl presignDownload(String storageKey, String originalFileName);

    /** 저장소에 객체가 실제로 있는지 확인한다(완료 통보 §2 검증). 없으면 empty, 있으면 크기를 담는다. */
    Optional<StoredObject> head(String storageKey);

    /** presigned URL 과 만료 시각. */
    record PresignedUrl(String url, Instant expiresAt) {
    }

    /** HEAD 조회 결과 — 크기 대조용. */
    record StoredObject(long sizeBytes) {
    }
}
