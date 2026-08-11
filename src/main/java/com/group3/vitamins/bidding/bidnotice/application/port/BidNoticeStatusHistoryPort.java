package com.group3.vitamins.bidding.bidnotice.application.port;

import com.group3.vitamins.bidding.bidnotice.domain.model.BidNoticeStatusHistory;

public interface BidNoticeStatusHistoryPort {

    void save(BidNoticeStatusHistory history);
}
