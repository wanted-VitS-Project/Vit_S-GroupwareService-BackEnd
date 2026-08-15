package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.CollectedBidNoticeUpsertMapper;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.NoticeIdRow;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.NoticeUpsertRow;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.RawKeyPair;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.RawRecordKey;
import com.group3.vitamins.bidding.bidnotice.application.port.CompanyBidNoticeStatePort;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionSource;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionSourceRepository;
import com.group3.vitamins.bidding.collectionrun.application.model.*;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectedBidNoticeStorePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaCollectedBidNoticeStoreAdapter
        implements CollectedBidNoticeStorePort {

    private final CollectionSourceRepository sourceRepository;
    private final CollectedBidNoticeUpsertMapper upsertMapper;
    private final BidNoticeAttachmentSynchronizer attachmentSynchronizer;
    private final CompanyBidNoticeStatePort companyStatePort;

    // 공고, 원문, 첨부파일을 같은 트랜잭션에서 배치로 저장합니다.
    @Override
    @Transactional
    public StoreResult saveAll(
            Long companyId,
            String sourceCode,
            Long runId,
            List<CollectedBidNoticePayload> payloads,
            LocalDateTime crawledAt
    ) {
        if (payloads == null || payloads.isEmpty()) {
            return new StoreResult(0, 0, 0);
        }

        CollectionSource source = sourceRepository
                .findNotDeletedByCode(sourceCode)
                .filter(CollectionSource::isAvailable)
                .orElseThrow(() -> new IllegalStateException(
                        "사용 가능한 수집처를 찾을 수 없습니다: " + sourceCode
                ));

        List<CollectedBidNotice> notices = payloads.stream()
                .map(CollectedBidNoticePayload::notice)
                .toList();

        // upsert 전 존재 여부로 신규·기존을 먼저 구분한다 — 이전에는 INSERT IGNORE의
        // 영향받은 행수로 이걸 판단했는데, 배치 upsert는 행수만으로 신규/기존을 구분 못 한다.
        Set<NoticeKey> existingBeforeUpsert = toNoticeKeySet(
                upsertMapper.findNoticeIds(source.sourceId(), notices)
        );

        List<NoticeUpsertRow> upsertRows = payloads.stream()
                .map(payload -> new NoticeUpsertRow(
                        payload.notice(), payload.notice().hasAttachments()
                ))
                .toList();
        upsertMapper.upsertNotices(source.sourceId(), upsertRows, crawledAt);

        // upsert 직후 전체 키의 실제 ID를 한 번에 조회한다 (신규 생성된 ID 포함).
        Map<NoticeKey, Long> noticeIdsByKey = upsertMapper
                .findNoticeIds(source.sourceId(), notices)
                .stream()
                .collect(Collectors.toMap(
                        row -> new NoticeKey(row.externalId(), row.noticeOrder()),
                        NoticeIdRow::bidNoticeId
                ));

        List<RawKeyPair> rawKeysToCheck = payloads.stream()
                .map(payload -> new RawKeyPair(
                        requireNoticeId(noticeIdsByKey, payload.notice()),
                        payload.rawPayloadHash()
                ))
                .toList();
        Set<RawKeyPair> existingRawKeys =
                new HashSet<>(upsertMapper.findExistingRawKeys(rawKeysToCheck));

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        Set<Long> observedNoticeIds = new LinkedHashSet<>();
        Set<Long> touchOnlyNoticeIds = new LinkedHashSet<>();
        List<RawRecordKey> newRawRecords = new ArrayList<>();
        Map<Long, List<CollectedBidNotice.Attachment>> attachments =
                new HashMap<>();

        for (CollectedBidNoticePayload payload : payloads) {
            CollectedBidNotice notice = payload.notice();
            Long noticeId = requireNoticeId(noticeIdsByKey, notice);
            observedNoticeIds.add(noticeId);

            RawKeyPair rawKey = new RawKeyPair(noticeId, payload.rawPayloadHash());
            if (existingRawKeys.contains(rawKey)) {
                touchOnlyNoticeIds.add(noticeId);
                skipped++;
                continue;
            }

            newRawRecords.add(new RawRecordKey(
                    noticeId, payload.rawPayload(), payload.rawPayloadHash()
            ));

            if (existingBeforeUpsert.contains(
                    new NoticeKey(notice.externalId(), notice.noticeOrder())
            )) {
                updated++;
            } else {
                inserted++;
            }
            attachments.put(noticeId, notice.attachments());
        }

        if (!newRawRecords.isEmpty()) {
            upsertMapper.insertRawRecords(newRawRecords, runId, sourceCode, crawledAt);
        }
        if (!touchOnlyNoticeIds.isEmpty()) {
            upsertMapper.touchObservedAtBulk(touchOnlyNoticeIds, crawledAt);
        }

        attachmentSynchronizer.synchronize(attachments, crawledAt);
        companyStatePort.observeAll(
                companyId,
                List.copyOf(observedNoticeIds),
                runId,
                crawledAt
        );

        return new StoreResult(inserted, updated, skipped);
    }

    private Set<NoticeKey> toNoticeKeySet(List<NoticeIdRow> rows) {
        Set<NoticeKey> keys = new HashSet<>();
        for (NoticeIdRow row : rows) {
            keys.add(new NoticeKey(row.externalId(), row.noticeOrder()));
        }
        return keys;
    }

    private Long requireNoticeId(
            Map<NoticeKey, Long> noticeIdsByKey,
            CollectedBidNotice notice
    ) {
        Long noticeId = noticeIdsByKey.get(
                new NoticeKey(notice.externalId(), notice.noticeOrder())
        );
        if (noticeId == null) {
            throw new IllegalStateException("원자 저장된 입찰 공고를 찾을 수 없습니다.");
        }
        return noticeId;
    }

    private record NoticeKey(String externalId, String noticeOrder) {
    }
}
