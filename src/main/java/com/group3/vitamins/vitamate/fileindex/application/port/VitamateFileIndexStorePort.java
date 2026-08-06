package com.group3.vitamins.vitamate.fileindex.application.port;

import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;

import java.time.LocalDateTime;

// file_index 테이블 저장과 갱신을 담당하는 포트
public interface VitamateFileIndexStorePort {

    boolean existsFileVersion(Long fileVersionId);

    FileIndexStatus upsertStatus(
            Long fileVersionId,
            FileIndexStatus indexStatus,
            String errorMessage,
            LocalDateTime now
    );
}