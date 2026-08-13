package com.group3.vitamins.bidding.bidreview.application.port;

import java.time.LocalDateTime;
import java.util.List;

public interface BidReviewExpiryScanPort {

    // 만료 후보 ID 목록(잠금 없음).
    List<Long> findExpiredCandidateIds(LocalDateTime now, int batchSize);

    // 후보 하나를 점유하고 정리 Outbox를 저장한다. 실제로 점유했으면 true(스캔~점유 사이 상태가
    // 바뀌었으면 false — 예: 그 사이 프로젝트로 귀속됨, 이미 다른 서버가 점유함).
    boolean claimAndRequestCleanup(Long reviewId, LocalDateTime now);
}