package com.group3.vitamins.bidding.bidreview.domain.repository;

import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;

import java.util.List;
import java.util.Optional;

public interface BidReviewRepository {

    // 소유권과 무관하게 검토 존재 여부만 확인합니다. 소유권 검증은 호출자가 직접 한다
    // (404 대신 403을 내려야 하는 곳이 있어, 스코프를 걸어 존재 자체를 숨기지 않는다).
    Optional<BidReview> findById(Long reviewId);

    // 검토에 저장된 문서 목록을 조회합니다.
    List<BidReviewDocument> findDocumentsByReviewId(Long reviewId);

    // 같은 사용자가 같은 공고를 현재 검토 중인지 확인합니다.
    boolean existsProcessingReview(
            Long companyId,
            Long noticeId,
            String requestedBy
    );

    // Worker의 현재 처리 시도와 일치하는 검토를 조회합니다.
    Optional<BidReview> findByIdAndAttemptId(
            Long reviewId,
            String attemptId
    );
}