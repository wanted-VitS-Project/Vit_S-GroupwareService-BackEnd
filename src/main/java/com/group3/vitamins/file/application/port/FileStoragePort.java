package com.group3.vitamins.file.application.port;

import java.time.Instant;
import java.util.Collection;
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

    /** 객체 바이트를 가져온다(§2 PDF 페이지 수 추출용 서버 다운로드). PDF 만 대상이라 크기가 제한적이다. */
    byte[] getObject(String storageKey);

    /**
     * 여러 객체를 영구 삭제한다(§7 영구삭제 — 문서의 전 버전 저장소 객체 제거). 실제로 삭제된 객체 수를 돌려준다.
     * ⛔ <b>일부/전체 실패해도 예외를 던지지 않는다</b> — DB 삭제는 이미 끝났고 실패 키는 정리 대상으로 남긴다(§7).
     */
    int deleteObjects(Collection<String> storageKeys);

    /** presigned URL 과 만료 시각. */
    record PresignedUrl(String url, Instant expiresAt) {
    }

    /** HEAD 조회 결과 — 크기 대조용. */
    record StoredObject(long sizeBytes) {
    }
}
