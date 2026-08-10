package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePage;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePayload;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunFailureType;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeApiResponse;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeSearchRequest;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeClientException;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeNormalizationException;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.mapper.NaraBidNoticePayloadMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NaraCollectionSourceAdapter 단건 조합 수집")
class NaraCollectionSourceAdapterTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 8, 9, 6, 0);
    private static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 8, 10, 6, 0);
    private static final CollectionRequestCombination TARGET =
            new CollectionRequestCombination(BidNoticeType.SERVICE, "스마트시티", "11", "6202", 1);

    @Mock
    private NaraBidNoticeClient client;
    @Mock
    private NaraBidNoticePayloadMapper payloadMapper;

    private NaraCollectionSourceAdapter adapter;
    private JsonNode rawItem;
    private CollectedBidNoticePayload payload;

    @BeforeEach
    void setUp() {
        adapter = new NaraCollectionSourceAdapter(client, payloadMapper);
        rawItem = new ObjectMapper().createObjectNode().put("bidNtceNo", "NOTICE-001");
        payload = payload();
    }

    @Test
    @DisplayName("task가 지정한 요청 조합 한 건만 수집한다")
    void collectsTargetCombination() {
        when(client.searchServiceNotices(any()))
                .thenReturn(response(List.of(rawItem), 1, 100, 1));
        when(payloadMapper.map(rawItem, BidNoticeType.SERVICE)).thenReturn(payload);

        CollectedBidNoticePage result = adapter.collect(snapshot("NARA"), TARGET, 100);

        assertThat(result.notices()).containsExactly(payload);
        assertThat(result.failures()).isEmpty();
        ArgumentCaptor<NaraBidNoticeSearchRequest> captor =
                ArgumentCaptor.forClass(NaraBidNoticeSearchRequest.class);
        verify(client).searchServiceNotices(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("스마트시티");
        assertThat(captor.getValue().regionCode()).isEqualTo("11");
        assertThat(captor.getValue().industryCode()).isEqualTo("6202");
    }

    @Test
    @DisplayName("외부 API 연결 실패는 재시도 가능한 실패로 반환한다")
    void returnsRetryableFailure() {
        when(client.searchServiceNotices(any()))
                .thenThrow(new NaraBidNoticeClientException(
                        "temporary failure",
                        true,
                        new RuntimeException("connection failed")
                ));

        CollectedBidNoticePage result = adapter.collect(snapshot("NARA"), TARGET, 100);

        assertThat(result.notices()).isEmpty();
        assertThat(result.failures().get(0).failureType())
                .isEqualTo(CollectionRunFailureType.CONNECTION_FAILURE);
        assertThat(result.failures().get(0).retryable()).isTrue();
    }

    @Test
    @DisplayName("응답 변환 실패는 재시도하지 않는 실패로 반환한다")
    void returnsPermanentNormalizationFailure() {
        when(client.searchServiceNotices(any()))
                .thenReturn(response(List.of(rawItem), 1, 100, 1));
        when(payloadMapper.map(rawItem, BidNoticeType.SERVICE))
                .thenThrow(new NaraBidNoticeNormalizationException("invalid response"));

        CollectedBidNoticePage result = adapter.collect(snapshot("NARA"), TARGET, 100);

        assertThat(result.failures().get(0).failureType())
                .isEqualTo(CollectionRunFailureType.RESPONSE_PARSING_FAILURE);
        assertThat(result.failures().get(0).retryable()).isFalse();
    }

    @Test
    @DisplayName("지원하지 않는 수집처는 외부 API 호출 전에 거부한다")
    void rejectsUnsupportedSource() {
        assertThatThrownBy(() -> adapter.collect(snapshot("KCAA"), TARGET, 100))
                .isInstanceOf(IllegalArgumentException.class);
        verify(client, never()).searchServiceNotices(any());
    }

    private CollectionRunConditionSnapshot snapshot(String sourceCode) {
        return new CollectionRunConditionSnapshot(
                sourceCode,
                "테스트 수집",
                List.of(BidNoticeType.SERVICE),
                new CollectionConditionFilter(
                        List.of("스마트시티"), List.of("11"), List.of("6202"),
                        null, null, true, null
                ),
                STARTED_AT,
                ENDED_AT
        );
    }

    private NaraBidNoticeApiResponse response(
            List<JsonNode> items, int pageNumber, int pageSize, int totalCount
    ) {
        return new NaraBidNoticeApiResponse(new NaraBidNoticeApiResponse.Response(
                new NaraBidNoticeApiResponse.Header("00", "정상"),
                new NaraBidNoticeApiResponse.Body(items, pageSize, pageNumber, totalCount)
        ));
    }

    private CollectedBidNoticePayload payload() {
        return new CollectedBidNoticePayload(
                new CollectedBidNotice(
                        "NOTICE-001", "00", BidNoticeType.SERVICE, "테스트 공고",
                        null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, List.of()
                ),
                "{\"bidNtceNo\":\"NOTICE-001\"}",
                "a".repeat(64)
        );
    }
}
