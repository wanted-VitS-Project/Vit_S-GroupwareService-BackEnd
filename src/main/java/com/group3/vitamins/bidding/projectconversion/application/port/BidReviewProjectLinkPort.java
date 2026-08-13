package com.group3.vitamins.bidding.projectconversion.application.port;

import java.util.Optional;

// 공고 프로젝트 전환이 bid_review를 읽고(2·4번) 나중에 project_id를 쓰기(10번) 위한 포트.
// ⚠️ bidreview 자체 도메인 리포지토리(BidReviewRepository)를 직접 주입하지 않는다 - 소비자가
// 자기 포트를 갖는 이 코드베이스의 기존 관례를 따른다(BidNoticeProjectAccessPort와 동일 결정).
public interface BidReviewProjectLinkPort {

    Optional<ReviewSnapshot> findReview(Long reviewId);

    record ReviewSnapshot(
            Long reviewId,
            Long companyId,
            Long noticeId,
            String requestedBy,
            String reviewStatus,
            Long projectId
    ) {
    }
}
