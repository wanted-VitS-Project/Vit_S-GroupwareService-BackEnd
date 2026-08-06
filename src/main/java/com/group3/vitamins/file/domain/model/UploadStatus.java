package com.group3.vitamins.file.domain.model;

/**
 * 파일 버전 업로드 상태 (`file_version.upload_status`).
 *
 * <p>presigned 업로드는 서버가 관여하지 않는 구간(클라이언트 → S3 직접 PUT)이 있어
 * 버전이 자체 상태를 가진다 — 이것이 파일이 블록 상세와 달리 독립 생명주기를 갖는 이유다.
 *
 * <pre>
 *   UPLOADING  업로드 시작(§1) 직후. presigned 발급됨, 아직 완료 통보 전
 *   COMPLETED  완료 통보(§2)에서 S3 HEAD 검증 통과 → 조회·다운로드·미리보기 가능
 *   FAILED     완료 통보 시 저장소에 객체가 없거나 크기/체크섬 불일치
 * </pre>
 */
public enum UploadStatus {
    UPLOADING,
    COMPLETED,
    FAILED
}
