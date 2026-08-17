package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.application.port.CompanyBidNoticeStatePort;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.CollectedBidNoticeUpsertMapper;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.NoticeIdRow;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.NoticeUpsertRow;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.RawKeyPair;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.RawRecordKey;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionSource;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionSourceRepository;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePayload;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectedBidNoticeStorePort.StoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("JpaCollectedBidNoticeStoreAdapter 배치 저장")
class JpaCollectedBidNoticeStoreAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long RUN_ID = 20L;
    private static final Long SOURCE_ID = 1L;
    private static final String SOURCE_CODE = "NARA";
    private static final LocalDateTime CRAWLED_AT =
            LocalDateTime.of(2026, 8, 16, 10, 0);

    private CollectionSourceRepository sourceRepository;
    private CollectedBidNoticeUpsertMapper upsertMapper;
    private BidNoticeAttachmentSynchronizer attachmentSynchronizer;
    private CompanyBidNoticeStatePort companyStatePort;
    private JpaCollectedBidNoticeStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        sourceRepository = mock(CollectionSourceRepository.class);
        upsertMapper = mock(CollectedBidNoticeUpsertMapper.class);
        attachmentSynchronizer = mock(BidNoticeAttachmentSynchronizer.class);
        companyStatePort = mock(CompanyBidNoticeStatePort.class);

        adapter = new JpaCollectedBidNoticeStoreAdapter(
                sourceRepository, upsertMapper, attachmentSynchronizer, companyStatePort
        );

        when(sourceRepository.findNotDeletedByCode(SOURCE_CODE))
                .thenReturn(Optional.of(
                        new CollectionSource(SOURCE_ID, SOURCE_CODE, "나라장터", "OPEN_API", true)
                ));
    }

    @Test
    @DisplayName("빈 배치는 저장소를 전혀 건드리지 않고 (0,0,0)을 반환한다")
    void returnsEmptyResultWithoutTouchingStoreForEmptyPayloads() {
        StoreResult result = adapter.saveAll(
                COMPANY_ID, SOURCE_CODE, RUN_ID, List.of(), CRAWLED_AT
        );

        assertThat(result).isEqualTo(new StoreResult(0, 0, 0));
        verifyNoInteractions(upsertMapper, attachmentSynchronizer, companyStatePort);
    }

    @Test
    @DisplayName("upsert 전에 없던 공고는 신규(inserted)로 집계하고 원문·첨부를 반영한다")
    void countsBrandNewNoticeAsInserted() {
        CollectedBidNoticePayload payload = payload("EXT-1", "00");

        // upsert 전에는 존재하지 않음 → 두 번째 조회에서 새로 생성된 100번으로 확인됨
        when(upsertMapper.findNoticeIds(eq(SOURCE_ID), any()))
                .thenReturn(List.of())
                .thenReturn(List.of(new NoticeIdRow("EXT-1", "00", 100L)));
        when(upsertMapper.findExistingRawKeys(any())).thenReturn(List.of());

        StoreResult result = adapter.saveAll(
                COMPANY_ID, SOURCE_CODE, RUN_ID, List.of(payload), CRAWLED_AT
        );

        assertThat(result).isEqualTo(new StoreResult(1, 0, 0));

        ArgumentCaptor<List<RawRecordKey>> rawCaptor = ArgumentCaptor.forClass(List.class);
        verify(upsertMapper).insertRawRecords(
                rawCaptor.capture(), eq(RUN_ID), eq(SOURCE_CODE), eq(CRAWLED_AT)
        );
        assertThat(rawCaptor.getValue()).containsExactly(
                new RawRecordKey(100L, payload.rawPayload(), payload.rawPayloadHash())
        );

        verify(upsertMapper, never()).touchObservedAtBulk(any(), any());
        verify(attachmentSynchronizer).synchronize(
                eq(Map.of(100L, payload.notice().attachments())), eq(CRAWLED_AT)
        );
        verify(companyStatePort).observeAll(COMPANY_ID, List.of(100L), RUN_ID, CRAWLED_AT);
    }

    @Test
    @DisplayName("upsert 전에 이미 있던 공고는 갱신(updated)으로 집계한다")
    void countsExistingNoticeWithNewContentAsUpdated() {
        CollectedBidNoticePayload payload = payload("EXT-2", "00");

        when(upsertMapper.findNoticeIds(eq(SOURCE_ID), any()))
                .thenReturn(List.of(new NoticeIdRow("EXT-2", "00", 200L)))
                .thenReturn(List.of(new NoticeIdRow("EXT-2", "00", 200L)));
        when(upsertMapper.findExistingRawKeys(any())).thenReturn(List.of());

        StoreResult result = adapter.saveAll(
                COMPANY_ID, SOURCE_CODE, RUN_ID, List.of(payload), CRAWLED_AT
        );

        assertThat(result).isEqualTo(new StoreResult(0, 1, 0));
        verify(upsertMapper).insertRawRecords(any(), eq(RUN_ID), eq(SOURCE_CODE), eq(CRAWLED_AT));
    }

    @Test
    @DisplayName("원문이 이미 기록돼 있으면 skip 처리하고 notice 갱신·첨부 동기화는 하지 않는다")
    void skipsWhenRawAlreadyRecorded() {
        CollectedBidNoticePayload payload = payload("EXT-3", "00");

        when(upsertMapper.findNoticeIds(eq(SOURCE_ID), any()))
                .thenReturn(List.of(new NoticeIdRow("EXT-3", "00", 300L)))
                .thenReturn(List.of(new NoticeIdRow("EXT-3", "00", 300L)));
        when(upsertMapper.findExistingRawKeys(any()))
                .thenReturn(List.of(new RawKeyPair(300L, payload.rawPayloadHash())));

        StoreResult result = adapter.saveAll(
                COMPANY_ID, SOURCE_CODE, RUN_ID, List.of(payload), CRAWLED_AT
        );

        assertThat(result).isEqualTo(new StoreResult(0, 0, 1));
        verify(upsertMapper, never()).insertRawRecords(any(), any(), any(), any());
        verify(upsertMapper).touchObservedAtBulk(Set.of(300L), CRAWLED_AT);
        verify(attachmentSynchronizer).synchronize(eq(Map.of()), eq(CRAWLED_AT));
        // 원문은 skip이어도 회사 관측 상태는 여전히 갱신된다 (다시 확인했다는 사실 자체는 유효).
        verify(companyStatePort).observeAll(COMPANY_ID, List.of(300L), RUN_ID, CRAWLED_AT);
    }

    @Test
    @DisplayName("한 배치의 여러 공고를 마퍼 호출 1세트로 처리한다 (건별 반복 호출 아님)")
    void batchesMultipleNoticesIntoSingleMapperCallSet() {
        CollectedBidNoticePayload newPayload = payload("EXT-4", "00");
        CollectedBidNoticePayload skipPayload = payload("EXT-5", "00");

        when(upsertMapper.findNoticeIds(eq(SOURCE_ID), any()))
                .thenReturn(List.of(new NoticeIdRow("EXT-5", "00", 500L)))
                .thenReturn(List.of(
                        new NoticeIdRow("EXT-4", "00", 400L),
                        new NoticeIdRow("EXT-5", "00", 500L)
                ));
        when(upsertMapper.findExistingRawKeys(any()))
                .thenReturn(List.of(new RawKeyPair(500L, skipPayload.rawPayloadHash())));

        StoreResult result = adapter.saveAll(
                COMPANY_ID, SOURCE_CODE, RUN_ID,
                List.of(newPayload, skipPayload), CRAWLED_AT
        );

        assertThat(result).isEqualTo(new StoreResult(1, 0, 1));

        verify(upsertMapper, org.mockito.Mockito.times(2))
                .findNoticeIds(eq(SOURCE_ID), any());
        verify(upsertMapper).upsertNotices(eq(SOURCE_ID), any(), eq(CRAWLED_AT));
        verify(upsertMapper).findExistingRawKeys(any());
        verify(upsertMapper).insertRawRecords(any(), eq(RUN_ID), eq(SOURCE_CODE), eq(CRAWLED_AT));
        verify(upsertMapper).touchObservedAtBulk(Set.of(500L), CRAWLED_AT);
    }

    private CollectedBidNoticePayload payload(String externalId, String noticeOrder) {
        CollectedBidNotice notice = new CollectedBidNotice(
                externalId, noticeOrder, BidNoticeType.SERVICE,
                "테스트 공고 " + externalId, "테스트기관", null,
                "등록공고", "DOMESTIC",
                LocalDateTime.of(2026, 8, 15, 9, 0), null, null, null,
                null, null, null, null, null, null, null,
                "https://example.org/" + externalId, List.of()
        );
        String hash = "a".repeat(64);
        return new CollectedBidNoticePayload(
                notice, "{\"externalId\":\"" + externalId + "\"}", hash
        );
    }
}
