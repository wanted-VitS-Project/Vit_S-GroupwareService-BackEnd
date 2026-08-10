package com.group3.vitamins.bidding.collectioncondition.application.result;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;

import java.time.LocalDateTime;
import java.util.List;

// 수집 조건의 Application 계층 출력값입니다.
public record CollectionConditionResult(
        Long conditionId,
        String sourceCode,
        String sourceName,
        String conditionName,
        List<BidNoticeType> noticeTypes,
        CollectionConditionFilter filters,
        boolean active,
        LocalDateTime lastSuccessAt,
        Integer lastCollectedCount,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // 도메인 모델과 수집처 표시명을 API 출력값으로 변환합니다.
    public static CollectionConditionResult from(
            CollectionCondition condition,
            String sourceName
    ) {
        return new CollectionConditionResult(
                condition.getConditionId(),
                condition.getSourceCode(),
                sourceName,
                condition.getConditionName(),
                condition.getNoticeTypes(),
                condition.getFilters(),
                condition.isActive(),
                condition.getLastSuccessAt(),
                condition.getLastCollectedCount(),
                condition.getCreatedBy(),
                condition.getCreatedAt(),
                condition.getUpdatedAt()
        );
    }
}