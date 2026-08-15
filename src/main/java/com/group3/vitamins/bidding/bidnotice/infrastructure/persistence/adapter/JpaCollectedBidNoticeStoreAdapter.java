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

        // upsert 전 존재 여부로 신규·기존을 먼저 구분한다. 기존 공고의 ID는 upsert로 바뀌지
        // 않으므로, 원문 중복 여부도 이 시점의 ID로 미리 확인할 수 있다.
        Map<NoticeKey, Long> existingNoticeIdsByKey = queryNoticeIds(source.sourceId(), notices);

        // ⚠️ 신규 공고는 원문이 이미 있을 수 없다(같은 트랜잭션 전에는 notice_id 자체가 없었다) —
        // 그래서 기존 공고에 대해서만 원문 중복을 확인한다. 신규 공고까지 검사 대상에 넣으면
        // 이 시점엔 아직 ID가 없어 확인 자체가 불가능하다.
        Set<RawKeyPair> existingRawKeys = findExistingRawKeysForExistingNotices(
                payloads, existingNoticeIdsByKey
        );

        // 원문이 이미 기록된 "기존 공고"만 upsert 대상에서 뺀다 — 원래 계약대로 그 공고의
        // 필드·updated_at·deleted_at은 건드리지 않고 관측 시각만 갱신한다.
        List<CollectedBidNoticePayload> toUpsert = payloads.stream()
                .filter(payload -> !isRawAlreadyRecorded(payload, existingNoticeIdsByKey, existingRawKeys))
                .toList();

        if (!toUpsert.isEmpty()) {
            List<NoticeUpsertRow> upsertRows = toUpsert.stream()
                    .map(payload -> new NoticeUpsertRow(
                            payload.notice(), payload.notice().hasAttachments()
                    ))
                    .toList();
            upsertMapper.upsertNotices(source.sourceId(), upsertRows, crawledAt);
        }

        // upsert 이후 전체 키의 실제 ID를 다시 조회한다 (toUpsert로 새로 생성된 ID까지 포함).
        Map<NoticeKey, Long> noticeIdsByKey = queryNoticeIds(source.sourceId(), notices);

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

            if (isRawAlreadyRecorded(payload, existingNoticeIdsByKey, existingRawKeys)) {
                touchOnlyNoticeIds.add(noticeId);
                skipped++;
                continue;
            }

            newRawRecords.add(new RawRecordKey(
                    noticeId, payload.rawPayload(), payload.rawPayloadHash()
            ));

            boolean wasExisting = existingNoticeIdsByKey.containsKey(
                    new NoticeKey(notice.externalId(), notice.noticeOrder())
            );
            if (wasExisting) {
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

    private Map<NoticeKey, Long> queryNoticeIds(Long sourceId, List<CollectedBidNotice> notices) {
        return upsertMapper.findNoticeIds(sourceId, notices)
                .stream()
                .collect(Collectors.toMap(
                        row -> new NoticeKey(row.externalId(), row.noticeOrder()),
                        NoticeIdRow::bidNoticeId
                ));
    }

    // 기존 공고들의 (notice_id, raw_payload_hash) 중 이미 기록된 것을 조회한다.
    private Set<RawKeyPair> findExistingRawKeysForExistingNotices(
            List<CollectedBidNoticePayload> payloads,
            Map<NoticeKey, Long> existingNoticeIdsByKey
    ) {
        List<RawKeyPair> keysToCheck = payloads.stream()
                .map(payload -> {
                    Long existingId = existingNoticeIdsByKey.get(new NoticeKey(
                            payload.notice().externalId(), payload.notice().noticeOrder()
                    ));
                    return existingId == null
                            ? null
                            : new RawKeyPair(existingId, payload.rawPayloadHash());
                })
                .filter(Objects::nonNull)
                .toList();

        if (keysToCheck.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(upsertMapper.findExistingRawKeys(keysToCheck));
    }

    // 이 payload가 "이미 기록된 기존 공고의 원문과 동일한지" 판단한다. 신규 공고는 항상 false다.
    private boolean isRawAlreadyRecorded(
            CollectedBidNoticePayload payload,
            Map<NoticeKey, Long> existingNoticeIdsByKey,
            Set<RawKeyPair> existingRawKeys
    ) {
        Long existingId = existingNoticeIdsByKey.get(new NoticeKey(
                payload.notice().externalId(), payload.notice().noticeOrder()
        ));
        return existingId != null
                && existingRawKeys.contains(new RawKeyPair(existingId, payload.rawPayloadHash()));
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
