package com.group3.vitamins.bidding.bidsummary.infrastructure.query;

import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryItemResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BidNoticeSummaryHistoryQueryMapper {

    List<BidNoticeSummaryHistoryItemResult> findHistory(
            @Param("companyId") Long companyId,
            @Param("noticeId") Long noticeId,
            @Param("userId") String userId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countHistory(
            @Param("companyId") Long companyId,
            @Param("noticeId") Long noticeId,
            @Param("userId") String userId
    );

    Long findLatestMineSummaryId(
            @Param("companyId") Long companyId,
            @Param("noticeId") Long noticeId,
            @Param("userId") String userId
    );
}
