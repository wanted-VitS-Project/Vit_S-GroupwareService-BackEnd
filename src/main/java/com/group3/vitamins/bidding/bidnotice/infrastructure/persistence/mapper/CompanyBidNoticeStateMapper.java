package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Mapper
public interface CompanyBidNoticeStateMapper {

    // 동시 수집에서도 회사·공고 상태를 하나의 원자적 SQL로 생성하거나 갱신합니다.
    int upsertObserved(
            @Param("companyId") Long companyId,
            @Param("bidNoticeIds") Collection<Long> bidNoticeIds,
            @Param("runId") Long runId,
            @Param("observedAt") LocalDateTime observedAt
    );

    Optional<CompanyBidNoticeStateRow> findForUpdate(
            @Param("companyId") Long companyId,
            @Param("bidNoticeId") Long bidNoticeId
    );

    int updateStatus(
            @Param("companyId") Long companyId,
            @Param("bidNoticeId") Long bidNoticeId,
            @Param("noticeStatus") String noticeStatus,
            @Param("dismissReason") String dismissReason,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}
