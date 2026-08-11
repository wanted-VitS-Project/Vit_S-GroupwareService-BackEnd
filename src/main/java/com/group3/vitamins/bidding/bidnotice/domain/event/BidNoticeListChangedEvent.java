package com.group3.vitamins.bidding.bidnotice.domain.event;

import com.group3.vitamins.global.domain.event.DomainEvent;

public record BidNoticeListChangedEvent(Long companyId) implements DomainEvent {

    public BidNoticeListChangedEvent {
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("companyId must be positive");
        }
    }
}
