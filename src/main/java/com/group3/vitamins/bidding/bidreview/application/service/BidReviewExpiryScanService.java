package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewExpiryScanPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidReviewExpiryScanService {

    private final BidReviewExpiryScanPort scanPort;
    private final Clock clock;

    // 만료 후보를 찾아 하나씩 점유하고 정리 Outbox를 저장합니다. 실제로 점유한 건수를 반환합니다.
    public int scanBatch(int batchSize) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> candidateIds = scanPort.findExpiredCandidateIds(now, batchSize);

        int claimedCount = 0;
        for (Long reviewId : candidateIds) {
            if (scanPort.claimAndRequestCleanup(reviewId, LocalDateTime.now(clock))) {
                claimedCount++;
            }
        }

        return claimedCount;
    }
}