package com.group3.vitamins.bidding.collectionrun.presentation.api.response;

import com.group3.vitamins.bidding.collectionrun.application.result.CollectionRunResult;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CollectionRunResponse(

        @Schema(description = "수집 실행 ID", example = "1")
        Long runId,

        @Schema(description = "수집 조건 ID", example = "1")
        Long conditionId,

        @Schema(description = "실행 방식", example = "MANUAL")
        CollectionRunTriggerType triggerType,

        @Schema(description = "수집 실행 상태", example = "COMPLETED")
        CollectionRunStatus runStatus,

        @Schema(description = "실제 조회한 시작 시각(공고게시일시 기준)")
        LocalDateTime collectionStartedAt,

        @Schema(description = "실제 조회한 종료 시각(공고게시일시 기준)")
        LocalDateTime collectionEndedAt,

        @Schema(description = "전체 수집 건수", example = "30")
        int collectedCount,

        @Schema(description = "신규 등록 건수", example = "20")
        int insertedCount,

        @Schema(description = "갱신 건수", example = "5")
        int updatedCount,

        @Schema(description = "제외 건수", example = "5")
        int skippedCount,

        @Schema(description = "실패 사유. 실패가 아니면 null")
        String errorMessage,

        @Schema(description = "수집 시작 시각")
        LocalDateTime startedAt,

        @Schema(description = "수집 종료 시각. 실행 중이면 null")
        LocalDateTime finishedAt
) {

    // 수집 실행 결과를 API 응답으로 변환합니다.
    public static CollectionRunResponse from(CollectionRunResult result) {
        return new CollectionRunResponse(
                result.runId(),
                result.conditionId(),
                result.triggerType(),
                result.runStatus(),
                result.collectionStartedAt(),
                result.collectionEndedAt(),
                result.collectedCount(),
                result.insertedCount(),
                result.updatedCount(),
                result.skippedCount(),
                result.errorMessage(),
                result.startedAt(),
                result.finishedAt()
        );
    }
}