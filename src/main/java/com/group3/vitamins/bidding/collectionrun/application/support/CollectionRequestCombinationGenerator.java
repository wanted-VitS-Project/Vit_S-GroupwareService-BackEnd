package com.group3.vitamins.bidding.collectionrun.application.support;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CollectionRequestCombinationGenerator {

    // 공고 유형과 선택 필터의 모든 조합을 첫 페이지 task로 생성합니다.
    public List<CollectionRequestCombination> generate(
            CollectionRunConditionSnapshot condition
    ) {
        return condition.noticeTypes().stream()
                .flatMap(noticeType -> valuesOrNull(condition.filters().keywords()).stream()
                        .flatMap(keyword -> valuesOrNull(condition.filters().regionCodes()).stream()
                                .flatMap(regionCode -> valuesOrNull(condition.filters().industryCodes()).stream()
                                        .map(industryCode -> new CollectionRequestCombination(
                                                noticeType,
                                                keyword,
                                                regionCode,
                                                industryCode,
                                                1
                                        )))))
                .distinct()
                .toList();
    }

    private List<String> valuesOrNull(List<String> values) {
        return values == null || values.isEmpty()
                ? Collections.singletonList(null)
                : values;
    }
}
