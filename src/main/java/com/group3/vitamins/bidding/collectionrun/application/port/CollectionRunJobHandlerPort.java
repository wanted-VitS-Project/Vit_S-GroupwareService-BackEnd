package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJob;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunJobResult;

public interface CollectionRunJobHandlerPort {

    // 수집 작업을 처리하고 재시도 가능 여부가 포함된 결과를 반환합니다.
    CollectionRunJobResult handle(CollectionRunJob job);
}
