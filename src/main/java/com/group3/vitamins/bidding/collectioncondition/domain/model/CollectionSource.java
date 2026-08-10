package com.group3.vitamins.bidding.collectioncondition.domain.model;

// 입찰 공고를 가져오는 외부 수집처의 마스터 정보입니다.
public record CollectionSource(
        Long sourceId,
        String sourceCode,
        String sourceName,
        String sourceType,
        boolean enabled
) {

    // 현재 수집 조건에서 사용할 수 있는 수집처인지 확인합니다.
    public boolean isAvailable() {
        return enabled;
    }
}