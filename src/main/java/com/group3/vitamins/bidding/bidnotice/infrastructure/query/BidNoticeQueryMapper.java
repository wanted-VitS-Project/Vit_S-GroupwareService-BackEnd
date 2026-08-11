package com.group3.vitamins.bidding.bidnotice.infrastructure.query;

import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeDetailResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListItemResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BidNoticeQueryMapper {

    List<BidNoticeListItemResult> findAll(
            @Param("companyId") Long companyId,
            @Param("query") SearchBidNoticesQuery query,
            @Param("offset") int offset
    );

    long count(
            @Param("companyId") Long companyId,
            @Param("query") SearchBidNoticesQuery query
    );

    BidNoticeDetailRow findDetail(
            @Param("companyId") Long companyId,
            @Param("noticeId") Long noticeId
    );

    List<BidNoticeDetailResult.Attachment> findAttachments(
            @Param("noticeId") Long noticeId
    );
}
