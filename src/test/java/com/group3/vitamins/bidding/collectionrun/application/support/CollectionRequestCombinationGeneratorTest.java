package com.group3.vitamins.bidding.collectionrun.application.support;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollectionRequestCombinationGenerator 요청 조합 생성")
class CollectionRequestCombinationGeneratorTest {

    private final CollectionRequestCombinationGenerator generator =
            new CollectionRequestCombinationGenerator();

    @Test
    @DisplayName("공고 유형과 필터의 모든 조합을 첫 페이지 task로 생성한다")
    void generatesCartesianProduct() {
        CollectionRunConditionSnapshot snapshot = new CollectionRunConditionSnapshot(
                "NARA",
                "테스트 조건",
                List.of(BidNoticeType.SERVICE, BidNoticeType.CONSTRUCTION),
                new CollectionConditionFilter(
                        List.of("스마트시티", "통합관제"),
                        List.of("11", "41"),
                        List.of(),
                        null, null, true, null
                ),
                LocalDateTime.of(2026, 8, 9, 0, 0),
                LocalDateTime.of(2026, 8, 10, 0, 0)
        );

        var result = generator.generate(snapshot);

        assertThat(result).hasSize(8);
        assertThat(result).allMatch(target -> target.pageNumber() == 1);
        assertThat(result).allMatch(target -> target.industryCode() == null);
    }
}
