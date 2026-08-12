package com.group3.vitamins.bidding.bidreview.application.port;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;

import java.util.List;

public interface BidReviewCommandPort {

    // 같은 회사·공고·요청자의 진행 중 검토가 있는지 확인합니다.
    boolean existsProcessing(
            Long companyId,
            Long noticeId,
            String requestedBy
    );

    // 검토, 선택 문서와 Worker 발행용 Outbox를 한 트랜잭션으로 저장합니다.
    BidReview savePendingWithDocumentsAndOutbox(
            BidReview review,
            List<BidReviewDocument> documents
    );

    // 종료된 검토 상태와 정리 요청 Outbox를 한 트랜잭션으로 저장합니다.
    BidReview saveAbandonedWithCleanupOutbox(BidReview review);
}