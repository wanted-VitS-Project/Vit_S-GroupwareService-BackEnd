package com.group3.vitamins.file.application.result;

/**
 * 입찰 검토 파일 귀속 결과 (FILE-V1 §2-G-1).
 *
 * <p>{@code indexStatus} 는 인덱싱이 비동기 큐 트리거라 <b>귀속 반환 시점의 초기 상태</b>다(완료 아님).
 * vitamate 인덱싱 초기값과 동일한 {@link #INDEX_PENDING}.
 */
public record AttachStagedFileResult(
        long fileId,
        long fileVersionId,
        int versionNo,
        String indexStatus) {

    /** 인덱싱 트리거 직후 초기 상태(vitamate FileIndexStatus 초기값과 일치). */
    public static final String INDEX_PENDING = "PENDING";
}
