package com.group3.vitamins.bidding.collectionrun.application.port;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskFailure;

public interface CollectionRunTaskDlqPort {

    // 최종 실패한 요청 조합을 별도 DLQ에 기록합니다.
    void publish(CollectionRunTaskFailure failure);
}
