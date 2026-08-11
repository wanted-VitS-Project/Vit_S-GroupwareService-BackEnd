package com.group3.vitamins.bidding.bidnotice.domain.model;

import java.time.LocalDateTime;

// 회사가 직접 등록한 입찰 공고의 소유권과 변경 가능한 내용을 관리합니다.
public class ManualBidNotice {

    public static final String SOURCE_CODE = "MANUAL";
    public static final String SOURCE_NAME = "직접 등록";
    public static final String DEFAULT_NOTICE_ORDER = "00";
    public static final String DEFAULT_NOTICE_STATUS = "COLLECTED";

    private final Long noticeId;
    private final Long ownerCompanyId;
    private final Long crawlSourceId;
    private final String externalId;
    private final String noticeOrder;
    private String manualDedupKey;
    private ManualBidNoticeData data;
    private final String noticeStatus;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ManualBidNotice(
            Long noticeId,
            Long ownerCompanyId,
            Long crawlSourceId,
            String externalId,
            String noticeOrder,
            String manualDedupKey,
            ManualBidNoticeData data,
            String noticeStatus,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.noticeId = noticeId;
        this.ownerCompanyId = ownerCompanyId;
        this.crawlSourceId = crawlSourceId;
        this.externalId = externalId;
        this.noticeOrder = noticeOrder;
        this.manualDedupKey = manualDedupKey;
        this.data = data;
        this.noticeStatus = noticeStatus;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 현재 회사 소유의 직접 등록 공고를 생성합니다.
    public static ManualBidNotice create(
            Long ownerCompanyId,
            Long crawlSourceId,
            String externalId,
            String manualDedupKey,
            ManualBidNoticeData data,
            String createdBy,
            LocalDateTime now
    ) {
        return new ManualBidNotice(
                null,
                ownerCompanyId,
                crawlSourceId,
                externalId,
                DEFAULT_NOTICE_ORDER,
                manualDedupKey,
                data,
                DEFAULT_NOTICE_STATUS,
                createdBy,
                now,
                null
        );
    }

    // DB에 저장된 직접 등록 공고를 도메인 객체로 복원합니다.
    public static ManualBidNotice restore(
            Long noticeId,
            Long ownerCompanyId,
            Long crawlSourceId,
            String externalId,
            String noticeOrder,
            String manualDedupKey,
            ManualBidNoticeData data,
            String noticeStatus,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new ManualBidNotice(
                noticeId,
                ownerCompanyId,
                crawlSourceId,
                externalId,
                noticeOrder,
                manualDedupKey,
                data,
                noticeStatus,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    // 검증과 PATCH 병합이 끝난 공고 내용과 중복 키를 함께 변경합니다.
    public void update(
            ManualBidNoticeData data,
            String manualDedupKey,
            LocalDateTime updatedAt
    ) {
        this.data = data;
        this.manualDedupKey = manualDedupKey;
        this.updatedAt = updatedAt;
    }

    public Long getNoticeId() {
        return noticeId;
    }

    public Long getOwnerCompanyId() {
        return ownerCompanyId;
    }

    public Long getCrawlSourceId() {
        return crawlSourceId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getNoticeOrder() {
        return noticeOrder;
    }

    public String getManualDedupKey() {
        return manualDedupKey;
    }

    public ManualBidNoticeData getData() {
        return data;
    }

    public String getNoticeStatus() {
        return noticeStatus;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
