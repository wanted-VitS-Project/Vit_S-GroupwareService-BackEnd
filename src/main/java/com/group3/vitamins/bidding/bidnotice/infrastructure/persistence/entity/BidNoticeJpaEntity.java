package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNotice;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeData;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "bid_notice",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bid_notice_source_external_ord",
                columnNames = {
                        "crawl_source_id",
                        "external_id",
                        "notice_ord"
                }
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidNoticeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_notice_id")
    private Long bidNoticeId;

    // 공용 수집처의 PK만 보관하여 collectioncondition Entity에 직접 의존하지 않습니다.
    @Column(name = "crawl_source_id", nullable = false)
    private Long crawlSourceId;

    // 직접 등록 공고의 소유 회사이며 외부 수집 공고는 공용이므로 null입니다.
    @Column(name = "owner_company_id")
    private Long ownerCompanyId;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(name = "notice_ord", nullable = false, length = 20)
    private String noticeOrder;

    // 같은 회사의 직접 등록 공고가 동시에 중복 저장되지 않도록 사용하는 키입니다.
    @Column(
            name = "manual_dedup_key",
            length = 64,
            columnDefinition = "CHAR(64)"
    )
    private String manualDedupKey;

    @Column(name = "notice_type", length = 20)
    private String noticeType;

    @Column(name = "notice_name", nullable = false, length = 1000)
    private String noticeName;

    @Column(name = "external_notice_status", length = 30)
    private String externalNoticeStatus;

    @Column(name = "international_bid_type", length = 20)
    private String internationalBidType;

    @Column(name = "notice_agency", length = 400)
    private String noticeAgency;

    @Column(name = "demand_agency", length = 400)
    private String demandAgency;

    @Column(name = "announced_at")
    private LocalDateTime announcedAt;

    @Column(name = "bid_start_at")
    private LocalDateTime bidStartAt;

    @Column(name = "bid_deadline_at")
    private LocalDateTime bidDeadlineAt;

    @Column(name = "opening_at")
    private LocalDateTime openingAt;

    @Column(name = "base_amount", precision = 19, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "estimated_amount", precision = 19, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "bid_method", length = 100)
    private String bidMethod;

    @Column(name = "contract_method", length = 100)
    private String contractMethod;

    @Column(name = "participation_qualification_text", length = 1000)
    private String participationQualificationText;

    @Column(name = "region_limit_text", length = 500)
    private String regionLimitText;

    @Column(name = "business_limit_text", length = 500)
    private String businessLimitText;

    @Column(name = "joint_contract_allowed")
    private Boolean jointContractAllowed;

    @Column(name = "joint_contract_text", length = 500)
    private String jointContractText;

    @Column(name = "evaluation_method", length = 100)
    private String evaluationMethod;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "has_attachment", nullable = false)
    private boolean hasAttachment;

    @Column(name = "notice_status", nullable = false, length = 20)
    private String noticeStatus;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    @Column(name = "created_by", length = 20, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 처음 수집된 공고 엔티티를 생성합니다.
    public static BidNoticeJpaEntity create(
            Long crawlSourceId,
            CollectedBidNotice notice,
            LocalDateTime crawledAt
    ) {
        BidNoticeJpaEntity entity = new BidNoticeJpaEntity();
        entity.crawlSourceId = crawlSourceId;
        entity.externalId = notice.externalId();
        entity.noticeOrder = notice.noticeOrder();
        entity.noticeStatus = "COLLECTED";
        entity.createdAt = crawledAt;
        entity.applyCollectedNotice(notice, crawledAt);
        return entity;
    }

    // 회사가 직접 입력한 공고를 신규 영속성 객체로 생성합니다.
    public static BidNoticeJpaEntity createManual(ManualBidNotice notice) {
        BidNoticeJpaEntity entity = new BidNoticeJpaEntity();
        entity.crawlSourceId = notice.getCrawlSourceId();
        entity.ownerCompanyId = notice.getOwnerCompanyId();
        entity.externalId = notice.getExternalId();
        entity.noticeOrder = notice.getNoticeOrder();
        entity.manualDedupKey = notice.getManualDedupKey();
        entity.noticeStatus = notice.getNoticeStatus();
        entity.createdBy = notice.getCreatedBy();
        entity.createdAt = notice.getCreatedAt();
        entity.applyManualData(notice.getData());
        return entity;
    }

    // 검증과 PATCH 병합이 끝난 직접 등록 공고 내용을 갱신합니다.
    public void updateManual(ManualBidNotice notice) {
        this.manualDedupKey = notice.getManualDedupKey();
        this.updatedAt = notice.getUpdatedAt();
        this.deletedAt = null;
        applyManualData(notice.getData());
    }

    // 동일 공고가 다시 수집되면 외부 원천에서 갱신 가능한 값을 반영합니다.
    public void updateFrom(
            CollectedBidNotice notice,
            LocalDateTime crawledAt
    ) {
        applyCollectedNotice(notice, crawledAt);
        this.updatedAt = crawledAt;
        this.deletedAt = null;
    }
    // 내용 변경 없이 다시 확인된 공고의 마지막 수집 시각을 갱신합니다.
    public void markObserved(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }

    // 수집 결과에서 공통으로 갱신하는 필드를 한곳에서 관리합니다.
    private void applyCollectedNotice(
            CollectedBidNotice notice,
            LocalDateTime crawledAt
    ) {
        this.noticeType = notice.noticeType() == null
                ? null
                : notice.noticeType().name();
        this.noticeName = notice.noticeName();
        this.externalNoticeStatus = notice.externalNoticeStatus();
        this.internationalBidType = notice.internationalBidType();
        this.noticeAgency = notice.noticeAgency();
        this.demandAgency = notice.demandAgency();
        this.announcedAt = notice.announcedAt();
        this.bidStartAt = notice.bidStartAt();
        this.bidDeadlineAt = notice.bidDeadlineAt();
        this.openingAt = notice.openingAt();
        this.baseAmount = notice.baseAmount();
        this.estimatedAmount = notice.estimatedAmount();
        this.bidMethod = notice.bidMethod();
        this.contractMethod = notice.contractMethod();
        this.participationQualificationText =
                notice.participationQualificationText();
        this.jointContractAllowed = notice.jointContractAllowed();
        this.jointContractText = notice.jointContractText();
        this.sourceUrl = notice.sourceUrl();
        this.hasAttachment = notice.hasAttachments();
        this.crawledAt = crawledAt;
    }

    // 직접 등록 도메인의 사용자 입력 필드를 공용 공고 컬럼에 반영합니다.
    private void applyManualData(ManualBidNoticeData data) {
        this.noticeType = data.noticeType().name();
        this.noticeName = data.noticeName();
        this.externalNoticeStatus = null;
        this.internationalBidType = data.internationalBidType() == null
                ? null
                : data.internationalBidType().name();
        this.noticeAgency = data.noticeAgency();
        this.demandAgency = data.demandAgency();
        this.announcedAt = data.announcedAt();
        this.bidStartAt = data.bidStartAt();
        this.bidDeadlineAt = data.bidDeadlineAt();
        this.openingAt = data.openingAt();
        this.baseAmount = data.baseAmount();
        this.estimatedAmount = data.estimatedAmount();
        this.bidMethod = data.bidMethod();
        this.contractMethod = data.contractMethod();
        this.participationQualificationText =
                data.participationQualificationText();
        this.regionLimitText = data.regionLimitText();
        this.businessLimitText = data.businessLimitText();
        this.jointContractAllowed = data.jointContractAllowed();
        this.jointContractText = data.jointContractText();
        this.evaluationMethod = data.evaluationMethod();
        this.sourceUrl = data.sourceUrl();
        this.hasAttachment = !data.attachments().isEmpty();
        this.crawledAt = null;
    }
}
