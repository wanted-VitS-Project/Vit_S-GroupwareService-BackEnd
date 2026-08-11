package com.group3.vitamins.bidding.collectionrun.application.port;

import java.time.LocalDateTime;

public interface CollectionConditionResultPort {

    // 성공한 수집 실행의 시각과 수집 건수를 조건에 기록합니다.
    void recordSuccess(
            Long conditionId,
            LocalDateTime successAt,
            int collectedCount
    );
}
