package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeStatusHistoryPort;
import com.group3.vitamins.bidding.bidnotice.domain.model.BidNoticeStatusHistory;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeStatusHistoryJpaEntity;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository.SpringDataBidNoticeStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaBidNoticeStatusHistoryAdapter implements BidNoticeStatusHistoryPort {

    private final SpringDataBidNoticeStatusHistoryRepository repository;

    @Override
    public void save(BidNoticeStatusHistory history) {
        repository.save(BidNoticeStatusHistoryJpaEntity.from(history));
    }
}
