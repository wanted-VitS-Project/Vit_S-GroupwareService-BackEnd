package com.group3.vitamins.vitamate.application.port;

import java.time.LocalDateTime;

// 비타메이트 분석 작업을 외부 작업 큐에 발행하는 포트
public interface VitamateAnalysisJobPublisherPort {

    // 분석 작업 메시지를 큐에 발행한다.
    void publish(AnalysisJob job);

    // 큐에 전달할 최소 분석 작업 메시지
    record AnalysisJob(
            Long analysisId,
            String attemptId,
            int retryCount,
            LocalDateTime createdAt
    ) {
    }
}
