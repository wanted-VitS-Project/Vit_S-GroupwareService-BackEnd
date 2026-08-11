package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeCommandPort;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNotice;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeData;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeAttachmentJpaEntity;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeJpaEntity;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository.SpringDataBidNoticeAttachmentRepository;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository.SpringDataBidNoticeRepository;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionSource;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionSourceRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 직접 등록 공고와 첨부 링크를 JPA로 저장하고 회사 소유권을 보장합니다.
@Repository
@RequiredArgsConstructor
public class JpaBidNoticeCommandAdapter implements BidNoticeCommandPort {

    private static final String MANUAL_SOURCE_CODE = "MANUAL";

    private final CollectionSourceRepository sourceRepository;
    private final SpringDataBidNoticeRepository noticeRepository;
    private final SpringDataBidNoticeAttachmentRepository attachmentRepository;

    @Override
    public Optional<Long> findManualSourceId() {
        return sourceRepository.findNotDeletedByCode(MANUAL_SOURCE_CODE)
                .filter(CollectionSource::isAvailable)
                .map(CollectionSource::sourceId);
    }

    @Override
    public Optional<ManualBidNotice> findOwnedManualNotice(
            Long companyId,
            Long noticeId
    ) {
        return findManualSourceId()
                .flatMap(sourceId -> noticeRepository
                        .findByBidNoticeIdAndOwnerCompanyIdAndCrawlSourceIdAndDeletedAtIsNull(
                                noticeId,
                                companyId,
                                sourceId
                        ))
                .map(this::toDomain);
    }

    @Override
    public boolean existsExternalNotice(Long noticeId) {
        return noticeRepository
                .existsByBidNoticeIdAndOwnerCompanyIdIsNullAndDeletedAtIsNull(
                        noticeId
                );
    }

    @Override
    public boolean existsActiveDuplicate(
            Long companyId,
            String manualDedupKey,
            Long excludedNoticeId
    ) {
        if (excludedNoticeId == null) {
            return noticeRepository
                    .existsByOwnerCompanyIdAndManualDedupKeyAndDeletedAtIsNull(
                            companyId,
                            manualDedupKey
                    );
        }
        return noticeRepository
                .existsByOwnerCompanyIdAndManualDedupKeyAndBidNoticeIdNotAndDeletedAtIsNull(
                        companyId,
                        manualDedupKey,
                        excludedNoticeId
                );
    }

    @Override
    @Transactional
    public ManualBidNotice save(ManualBidNotice notice) {
        try {
            BidNoticeJpaEntity entity = notice.getNoticeId() == null
                    ? BidNoticeJpaEntity.createManual(notice)
                    : loadAndUpdate(notice);
            BidNoticeJpaEntity saved = noticeRepository.save(entity);
            synchronizeAttachments(
                    saved.getBidNoticeId(),
                    notice.getData().attachments(),
                    resolveChangedAt(notice)
            );
            return restore(saved, notice.getData().attachments());
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_MANUAL_NOTICE_DUPLICATED
            );
        }
    }

    // 수정 대상 Entity를 다시 조회하고 검증된 도메인 값을 반영합니다.
    private BidNoticeJpaEntity loadAndUpdate(ManualBidNotice notice) {
        BidNoticeJpaEntity entity = noticeRepository
                .findByBidNoticeIdAndOwnerCompanyIdAndCrawlSourceIdAndDeletedAtIsNull(
                        notice.getNoticeId(),
                        notice.getOwnerCompanyId(),
                        notice.getCrawlSourceId()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "저장할 직접 등록 공고를 찾을 수 없습니다."
                ));
        entity.updateManual(notice);
        return entity;
    }

    // UNIQUE 순번을 재사용하며 요청에서 빠진 기존 첨부만 논리 삭제합니다.
    private void synchronizeAttachments(
            Long noticeId,
            List<ManualBidNoticeAttachment> requested,
            LocalDateTime now
    ) {
        List<BidNoticeAttachmentJpaEntity> existing = attachmentRepository
                .findAllByBidNoticeIdOrderByAttachmentOrder(noticeId);
        Map<Integer, BidNoticeAttachmentJpaEntity> byOrder = new HashMap<>();
        existing.forEach(entity -> byOrder.put(
                entity.getAttachmentOrder().intValue(),
                entity
        ));

        for (ManualBidNoticeAttachment attachment : requested) {
            BidNoticeAttachmentJpaEntity entity =
                    byOrder.remove(attachment.attachmentOrder());
            if (entity == null) {
                entity = BidNoticeAttachmentJpaEntity.createManual(
                        noticeId,
                        attachment,
                        now
                );
            } else {
                entity.updateManual(attachment, now);
            }
            attachmentRepository.save(entity);
        }

        byOrder.values().forEach(entity -> entity.softDelete(now));
        attachmentRepository.saveAll(byOrder.values());
    }

    // 조회한 Entity와 활성 첨부 링크를 직접 등록 도메인으로 복원합니다.
    private ManualBidNotice toDomain(BidNoticeJpaEntity entity) {
        List<ManualBidNoticeAttachment> attachments = attachmentRepository
                .findAllByBidNoticeIdOrderByAttachmentOrder(entity.getBidNoticeId())
                .stream()
                .filter(attachment -> attachment.getDeletedAt() == null)
                .map(attachment -> new ManualBidNoticeAttachment(
                        attachment.getAttachmentOrder().intValue(),
                        attachment.getFileName(),
                        attachment.getSourceUrl()
                ))
                .toList();
        return restore(entity, attachments);
    }

    private ManualBidNotice restore(
            BidNoticeJpaEntity entity,
            List<ManualBidNoticeAttachment> attachments
    ) {
        return ManualBidNotice.restore(
                entity.getBidNoticeId(),
                entity.getOwnerCompanyId(),
                entity.getCrawlSourceId(),
                entity.getExternalId(),
                entity.getNoticeOrder(),
                entity.getManualDedupKey(),
                new ManualBidNoticeData(
                        entity.getNoticeName(),
                        BidNoticeType.valueOf(entity.getNoticeType()),
                        entity.getNoticeAgency(),
                        entity.getDemandAgency(),
                        enumOrNull(
                                InternationalBidType.class,
                                entity.getInternationalBidType()
                        ),
                        entity.getAnnouncedAt(),
                        entity.getBidStartAt(),
                        entity.getBidDeadlineAt(),
                        entity.getOpeningAt(),
                        entity.getBaseAmount(),
                        entity.getEstimatedAmount(),
                        entity.getBidMethod(),
                        entity.getContractMethod(),
                        entity.getParticipationQualificationText(),
                        entity.getRegionLimitText(),
                        entity.getBusinessLimitText(),
                        entity.getJointContractAllowed(),
                        entity.getJointContractText(),
                        entity.getEvaluationMethod(),
                        entity.getSourceUrl(),
                        attachments
                ),
                entity.getNoticeStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private LocalDateTime resolveChangedAt(ManualBidNotice notice) {
        return notice.getUpdatedAt() == null
                ? notice.getCreatedAt()
                : notice.getUpdatedAt();
    }

    private <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
