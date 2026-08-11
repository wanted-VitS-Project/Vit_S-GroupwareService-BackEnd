package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity;

import com.group3.vitamins.bidding.bidnotice.domain.model.BidNoticeStatusHistory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bid_notice_status_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BidNoticeStatusHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_notice_status_history_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "bid_notice_id", nullable = false)
    private Long bidNoticeId;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "changed_status", nullable = false, length = 20)
    private String changedStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_by", length = 20)
    private String changedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static BidNoticeStatusHistoryJpaEntity from(BidNoticeStatusHistory history) {
        BidNoticeStatusHistoryJpaEntity entity = new BidNoticeStatusHistoryJpaEntity();
        entity.companyId = history.companyId();
        entity.bidNoticeId = history.noticeId();
        entity.previousStatus = history.previousStatus() == null
                ? null : history.previousStatus().name();
        entity.changedStatus = history.changedStatus().name();
        entity.reason = history.reason();
        entity.changedBy = history.changedBy();
        entity.createdAt = history.createdAt();
        return entity;
    }
}
