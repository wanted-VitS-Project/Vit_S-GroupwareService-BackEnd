package com.group3.vitamins.vitamate.fileindex.application.port;

import java.time.LocalDateTime;

// 비타메이트 파일 인덱싱 작업을 외부 작업 큐에 발행하는 포트
public interface VitamateFileIndexJobPublisherPort {

    // 파일 인덱싱 작업 메시지를 큐에 발행한다.
    void publish(FileIndexJob job);

    // 큐에 전달할 최소 파일 인덱싱 작업 메시지
    record FileIndexJob(
            Long fileVersionId,
            int retryCount,
            LocalDateTime createdAt
    ) {
    }
}