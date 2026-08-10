package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeAttachmentJpaEntity;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository.SpringDataBidNoticeAttachmentRepository;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class BidNoticeAttachmentSynchronizer {

    private final SpringDataBidNoticeAttachmentRepository repository;

    // 변경된 공고들의 첨부파일을 최신 외부 응답과 동기화합니다.
    public void synchronize(
            Map<Long, List<CollectedBidNotice.Attachment>> incomingByNoticeId,
            LocalDateTime now
    ) {
        if (incomingByNoticeId.isEmpty()) {
            return;
        }

        Map<AttachmentKey, BidNoticeAttachmentJpaEntity> existing =
                new HashMap<>();

        repository.findAllByBidNoticeIdIn(
                incomingByNoticeId.keySet()
        ).forEach(entity -> existing.put(
                new AttachmentKey(
                        entity.getBidNoticeId(),
                        entity.getAttachmentOrder().intValue()
                ),
                entity
        ));

        List<BidNoticeAttachmentJpaEntity> changed = new ArrayList<>();

        incomingByNoticeId.forEach((noticeId, attachments) -> {
            // 외부 API가 첨부 목록을 생략한 경우 기존 첨부를 삭제된 것으로 해석하지 않습니다.
            if (attachments == null || attachments.isEmpty()) {
                existing.keySet().removeIf(key -> key.bidNoticeId().equals(noticeId));
                return;
            }

            for (CollectedBidNotice.Attachment attachment : attachments) {
                AttachmentKey key =
                        new AttachmentKey(noticeId, attachment.order());
                BidNoticeAttachmentJpaEntity entity = existing.remove(key);

                if (entity == null) {
                    entity = BidNoticeAttachmentJpaEntity.create(
                            noticeId,
                            attachment,
                            now
                    );
                } else {
                    entity.updateFrom(attachment, now);
                }
                changed.add(entity);
            }
        });

        // 최신 응답에서 사라진 기존 첨부파일을 논리 삭제합니다.
        existing.values().forEach(entity -> {
            entity.softDelete(now);
            changed.add(entity);
        });

        repository.saveAll(changed);
    }

    private record AttachmentKey(Long bidNoticeId, int order) {
    }
}
