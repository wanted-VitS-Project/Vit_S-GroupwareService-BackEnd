package com.group3.vitamins.bidding.bidnotice.application.usecase;

import com.group3.vitamins.bidding.bidnotice.application.query.GetBidNoticeDetailQuery;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeDetailResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListResult;

public interface BidNoticeQueryUseCase {
    BidNoticeListResult handle(SearchBidNoticesQuery query);
    BidNoticeDetailResult handle(GetBidNoticeDetailQuery query);
}
