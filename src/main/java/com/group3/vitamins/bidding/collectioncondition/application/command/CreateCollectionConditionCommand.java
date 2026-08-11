package com.group3.vitamins.bidding.collectioncondition.application.command;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;

import java.util.List;
import java.time.LocalTime;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionScheduleType;

// 새로운 입찰 공고 수집 조건 등록에 필요한 입력값입니다.
public record CreateCollectionConditionCommand(
        String sourceCode,
        String conditionName,
        List<BidNoticeType> noticeTypes,
        CollectionConditionFilter filters,
        Boolean active,
        Boolean autoCollectionEnabled,
        CollectionScheduleType scheduleType,
        LocalTime scheduledTime,
        String timezone,
        String userId,
        String role
) {
    public CreateCollectionConditionCommand(
            String sourceCode, String conditionName,
            List<BidNoticeType> noticeTypes, CollectionConditionFilter filters,
            Boolean active, String userId, String role
    ) {
        this(sourceCode, conditionName, noticeTypes, filters, active,
                false, null, null, null, userId, role);
    }
}
