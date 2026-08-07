package com.group3.vitamins.vitamate.fileindex.application.port;

import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;

import java.time.LocalDateTime;
import java.util.List;

// file_index 테이블 저장과 갱신을 담당하는 포트
public interface VitamateFileIndexStorePort {

    boolean existsFileVersion(Long fileVersionId);

    // PENDING으로 등록된 지 오래됐지만 진전이 없는 파일 버전 ID 목록을 찾는다 (재발행 스케줄러용).
    List<Long> findStalePendingFileVersionIds(LocalDateTime before);

    FileIndexStatusUpdateResult upsertStatus(
            Long fileVersionId,
            String indexAttemptId,
            FileIndexStatus indexStatus,
            String errorMessage,
            LocalDateTime now
    );

    // 파일 인덱싱 상태 저장 결과입니다. attempt가 어긋나면 accepted=false로 늦은 callback을 무시합니다.
    record FileIndexStatusUpdateResult(
            boolean accepted,
            String indexAttemptId,
            FileIndexStatus indexStatus,
            String reason
    ) {
    }
}
