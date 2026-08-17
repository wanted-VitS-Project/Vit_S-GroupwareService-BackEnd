package com.group3.vitamins.bidding.collectioncondition.presentation.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.group3.vitamins.bidding.collectioncondition.application.result.CollectionConditionResult;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionLookbackPeriod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.time.LocalTime;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionScheduleType;

public record CollectionConditionResponse(

        @Schema(description = "수집 조건 ID")
        Long conditionId,

        @Schema(description = "수집처 코드")
        String sourceCode,

        @Schema(description = "수집처 이름")
        String sourceName,

        @Schema(description = "수집 조건 이름")
        String conditionName,

        @Schema(description = "수집할 공고 유형")
        List<BidNoticeType> noticeTypes,

        @Schema(description = "공고 필터 설정")
        CollectionConditionFilterResponse filters,

        @Schema(description = "자동·수동 수집이 매 실행마다 되돌아가 검색할 기간")
        CollectionLookbackPeriod lookbackPeriod,

        @JsonProperty("isActive")
        @Schema(description = "수집 조건 활성화 여부")
        boolean active,

        @Schema(description = "자동 수집 사용 여부")
        boolean autoCollectionEnabled,

        @Schema(description = "자동 수집 주기")
        CollectionScheduleType scheduleType,

        @Schema(description = "자동 수집 실행 시각")
        LocalTime scheduledTime,

        @Schema(description = "자동 수집 기준 시간대")
        String timezone,

        @Schema(description = "다음 자동 수집 예정 시각")
        LocalDateTime nextRunAt,

        @Schema(description = "마지막 자동 수집 요청 생성 시각")
        LocalDateTime lastScheduledAt,

        @Schema(description = "마지막 수집 성공 시각")
        LocalDateTime lastSuccessAt,

        @Schema(description = "마지막 수집 건수")
        Integer lastCollectedCount,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {

    // 애플리케이션 조회 결과를 수집 조건 API 응답으로 변환합니다.
    public static CollectionConditionResponse from(
            CollectionConditionResult result
    ) {
        return new CollectionConditionResponse(
                result.conditionId(),
                result.sourceCode(),
                result.sourceName(),
                result.conditionName(),
                result.noticeTypes(),
                CollectionConditionFilterResponse.from(result.filters()),
                result.lookbackPeriod(),
                result.active(),
                result.autoCollectionEnabled(),
                result.scheduleType(),
                result.scheduledTime(),
                result.timezone(),
                result.nextRunAt(),
                result.lastScheduledAt(),
                result.lastSuccessAt(),
                result.lastCollectedCount(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
