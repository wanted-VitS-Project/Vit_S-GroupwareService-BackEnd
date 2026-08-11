package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.*;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository.*;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaCollectedBidNoticeStoreAdapter
        implements CollectedBidNoticeStorePort {

    private final CollectionSourceRepository sourceRepository;
    private final SpringDataBidNoticeRepository noticeRepository;
    private final SpringDataBidNoticeRawRepository rawRepository;
    private final BidNoticeAttachmentSynchronizer attachmentSynchronizer;
    private final CompanyBidNoticeStatePort companyStatePort;
    private final ObjectMapper objectMapper;

    // 공고, 원문, 첨부파일을 같은 트랜잭션에서 저장합니다.
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

        Map<NoticeKey, BidNoticeJpaEntity> notices =
                loadExistingNotices(source.sourceId(), payloads);
        Set<RawKey> existingRawKeys =
                loadExistingRawKeys(notices.values(), payloads);

        List<BidNoticeRawJpaEntity> newRawEntities = new ArrayList<>();
        Map<Long, List<CollectedBidNotice.Attachment>> attachments =
                new HashMap<>();

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (CollectedBidNoticePayload payload : payloads) {
            CollectedBidNotice notice = payload.notice();
            NoticeKey noticeKey = NoticeKey.from(notice);
            BidNoticeJpaEntity entity = notices.get(noticeKey);

            if (entity == null) {
                entity = noticeRepository.save(
                        BidNoticeJpaEntity.create(
                                source.sourceId(),
                                notice,
                                crawledAt
                        )
                );
                notices.put(noticeKey, entity);
                inserted++;
            } else {
                RawKey rawKey = new RawKey(
                        entity.getBidNoticeId(),
                        payload.rawPayloadHash()
                );

                if (existingRawKeys.contains(rawKey)) {
                    entity.markObserved(crawledAt);
                    skipped++;
                    continue;
                }

                entity.updateFrom(notice, crawledAt);
                updated++;
            }

            RawKey rawKey = new RawKey(
                    entity.getBidNoticeId(),
                    payload.rawPayloadHash()
            );

            newRawEntities.add(BidNoticeRawJpaEntity.create(
                    entity.getBidNoticeId(),
                    runId,
                    sourceCode,
                    parseRawPayload(payload.rawPayload()),
                    payload.rawPayloadHash(),
                    crawledAt
            ));
            existingRawKeys.add(rawKey);
            attachments.put(entity.getBidNoticeId(), notice.attachments());
        }

        rawRepository.saveAll(newRawEntities);
        attachmentSynchronizer.synchronize(attachments, crawledAt);
        companyStatePort.observeAll(
                companyId,
                notices.values().stream()
                        .map(BidNoticeJpaEntity::getBidNoticeId)
                        .toList(),
                runId,
                crawledAt
        );

        return new StoreResult(inserted, updated, skipped);
    }

    private Map<NoticeKey, BidNoticeJpaEntity> loadExistingNotices(
            Long sourceId,
            List<CollectedBidNoticePayload> payloads
    ) {
        Set<String> externalIds = payloads.stream()
                .map(payload -> payload.notice().externalId())
                .collect(Collectors.toSet());

        return noticeRepository
                .findAllByCrawlSourceIdAndExternalIdIn(sourceId, externalIds)
                .stream()
                .collect(Collectors.toMap(
                        NoticeKey::from,
                        Function.identity()
                ));
    }

    private Set<RawKey> loadExistingRawKeys(
            Collection<BidNoticeJpaEntity> notices,
            List<CollectedBidNoticePayload> payloads
    ) {
        Set<Long> noticeIds = notices.stream()
                .map(BidNoticeJpaEntity::getBidNoticeId)
                .collect(Collectors.toSet());

        if (noticeIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> hashes = payloads.stream()
                .map(CollectedBidNoticePayload::rawPayloadHash)
                .collect(Collectors.toSet());

        return rawRepository.findExistingRawKeys(noticeIds, hashes)
                .stream()
                .map(raw -> new RawKey(
                        raw.getBidNoticeId(),
                        raw.getRawPayloadHash()
                ))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private JsonNode parseRawPayload(String rawPayload) {
        try {
            return objectMapper.readTree(rawPayload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "입찰 공고 원문 JSON이 올바르지 않습니다.",
                    exception
            );
        }
    }

    private record NoticeKey(String externalId, String noticeOrder) {
        private static NoticeKey from(CollectedBidNotice notice) {
            return new NoticeKey(
                    notice.externalId(),
                    notice.noticeOrder()
            );
        }

        private static NoticeKey from(BidNoticeJpaEntity notice) {
            return new NoticeKey(
                    notice.getExternalId(),
                    notice.getNoticeOrder()
            );
        }
    }

    private record RawKey(Long bidNoticeId, String rawPayloadHash) {
    }
}
