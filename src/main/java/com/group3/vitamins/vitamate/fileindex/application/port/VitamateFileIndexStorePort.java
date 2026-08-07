package com.group3.vitamins.vitamate.fileindex.application.port;

import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;

import java.time.LocalDateTime;
import java.util.List;

// file_index 테이블 저장과 갱신을 담당하는 포트
public interface VitamateFileIndexStorePort {

    boolean existsFileVersion(Long fileVersionId);

    // PENDING으로 등록된 지 오래됐지만 진전이 없는 파일 버전 ID 후보를 제한된 개수만 찾는다
    // (재발행 스케줄러용). 후보일 뿐이며 재발행 전 claimStalePending으로 다시 확인해야 한다.
    List<Long> findStalePendingFileVersionIdCandidates(LocalDateTime before, int limit);

    // 후보 하나를 재확인하며 원자적으로 선점한다. true면 선점 성공(재발행해도 안전), false면
    // 그 사이 다른 경로(worker)가 이미 상태를 바꾼 것이므로 재발행하면 안 된다.
    boolean claimStalePending(Long fileVersionId, LocalDateTime before);

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
