package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePage;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;

public interface CollectionSourceCollectorPort {

    // 이 수집기가 처리할 수 있는 수집처 코드를 반환합니다.
    String supportedSourceCode();

    // 실행 시점에 고정된 조건으로 외부 수집처의 공고 한 페이지를 가져옵니다.
    CollectedBidNoticePage collect(
            CollectionRunConditionSnapshot condition,
            CollectionRequestCombination target,
            int pageSize
    );
}
