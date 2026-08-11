package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/**
 * 프로젝트 휴지통 모아보기(§13) projection (MyBatis 매핑 대상).
 * 삭제(휴지통)된 문서 1건 + 그 문서의 최신 완료 버전 표시정보 + 전체 버전 수 + 위치(스텝·블록) + 휴지통 진입 시각.
 * 블록도 함께 삭제된 고아 파일이면 {@code blockId}·{@code blockTitle} 이 {@code null} 이고 {@code blockDeleted=true} 이며,
 * {@code stepId}·{@code stepName} 은 삭제된 블록에 남은 {@code step_id} 로 해석한다.
 * §12(전체 모아보기)와 달리 latest 버전 ID·차수·업로더·미리보기 여부는 내리지 않는다(복구·영구삭제 전용 화면).
 */
public record ProjectTrashFileProjection(
        Long stepId,
        String stepName,
        Long blockId,
        String blockTitle,
        boolean blockDeleted,
        Long fileId,
        String name,
        int versionCount,
        String originalFileName,
        String extension,
        long sizeBytes,
        LocalDateTime deletedAt
) {
}
