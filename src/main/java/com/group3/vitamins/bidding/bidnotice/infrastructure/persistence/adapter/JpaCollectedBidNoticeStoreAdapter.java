package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper.CollectedBidNoticeUpsertMapper;
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

@Repository
@RequiredArgsConstructor
public class JpaCollectedBidNoticeStoreAdapter
        implements CollectedBidNoticeStorePort {

    private final CollectionSourceRepository sourceRepository;
    private final CollectedBidNoticeUpsertMapper upsertMapper;
    private final BidNoticeAttachmentSynchronizer attachmentSynchronizer;
    private final CompanyBidNoticeStatePort companyStatePort;

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

        Set<Long> observedNoticeIds = new LinkedHashSet<>();
        Map<Long, List<CollectedBidNotice.Attachment>> attachments =
                new HashMap<>();

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (CollectedBidNoticePayload payload : payloads) {
            CollectedBidNotice notice = payload.notice();
            int noticeInserted = upsertMapper.insertNoticeIfAbsent(
                    source.sourceId(), notice, notice.hasAttachments(), crawledAt
            );
            Long noticeId = requireNoticeId(source.sourceId(), notice);
            observedNoticeIds.add(noticeId);

            int rawInserted = upsertMapper.insertRawIfAbsent(
                    noticeId, runId, sourceCode, payload.rawPayload(),
                    payload.rawPayloadHash(), crawledAt
            );

            if (rawInserted == 0) {
                upsertMapper.touchObservedAt(noticeId, crawledAt);
                skipped++;
                continue;
            }

            if (noticeInserted == 1) {
                inserted++;
            } else {
                upsertMapper.updateCollectedNotice(
                        noticeId, notice, notice.hasAttachments(), crawledAt
                );
                updated++;
            }
            attachments.put(noticeId, notice.attachments());
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

    private Long requireNoticeId(Long sourceId, CollectedBidNotice notice) {
        Long noticeId = upsertMapper.findNoticeId(
                sourceId, notice.externalId(), notice.noticeOrder()
        );
        if (noticeId == null) {
            throw new IllegalStateException("원자 저장된 입찰 공고를 찾을 수 없습니다.");
        }
        return noticeId;
    }
}
