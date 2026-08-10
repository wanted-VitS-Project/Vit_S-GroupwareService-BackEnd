package com.group3.vitamins.bidding.collectionrun.presentation.api.response;

import com.group3.vitamins.bidding.collectionrun.application.result.CollectionRunResult;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record StartCollectionRunResponse(

        @Schema(description = "수집 실행 ID", example = "1")
        Long runId,

        @Schema(description = "수집 실행 상태", example = "PENDING")
        CollectionRunStatus runStatus,

        @Schema(description = "수집 요청 시각")
        LocalDateTime requestedAt
) {

    // 수집 실행 접수 결과를 API 응답으로 변환합니다.
    public static StartCollectionRunResponse from(CollectionRunResult result) {
        return new StartCollectionRunResponse(
                result.runId(),
                result.runStatus(),
                result.startedAt()
        );
    }
}