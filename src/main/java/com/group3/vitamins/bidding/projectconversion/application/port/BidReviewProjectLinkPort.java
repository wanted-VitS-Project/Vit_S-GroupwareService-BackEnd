package com.group3.vitamins.bidding.projectconversion.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 공고 프로젝트 전환이 bid_review를 읽고(2·4번) 나중에 project_id를 쓰기(10번) 위한 포트.
// ⚠️ bidreview 자체 도메인 리포지토리(BidReviewRepository)를 직접 주입하지 않는다 - 소비자가
// 자기 포트를 갖는 이 코드베이스의 기존 관례를 따른다(BidNoticeProjectAccessPort와 동일 결정).
public interface BidReviewProjectLinkPort {

    Optional<ReviewSnapshot> findReview(Long reviewId);

    // 10번 - 실제 다운로드에 성공한 공고 첨부(BID_ATTACHMENT + READY)만 귀속 대상이다. 사내
    // 기준자료·사내 문서함 참조는 Worker 다운로드를 거치지 않아 이 조건에 걸리지 않는다.
    List<PromotableDocument> findPromotableDocuments(Long reviewId);

    // 10번 - 귀속 성공을 반영한다. WHERE에 processing_status='READY'를 걸어 두 번 귀속되지
    // 않게 막는다(성공 true / 이미 다른 상태로 바뀌어 있어 갱신 안 됨 false).
    boolean markDocumentPromoted(Long reviewDocumentId, Long fileId, Long fileVersionId, LocalDateTime now);

    // 10번 - project_id를 조건부로 쓴다(WHERE review_status='COMPLETED' AND project_id IS NULL).
    // 성공하면 true, 이미 다른 프로젝트에 연결돼 있거나 상태가 바뀌었으면 false를 반환한다.
    boolean linkProject(Long reviewId, Long projectId, LocalDateTime now);

    record ReviewSnapshot(
            Long reviewId,
            Long companyId,
            Long noticeId,
            String requestedBy,
            String reviewStatus,
            Long projectId
    ) {
    }

    record PromotableDocument(
            Long reviewDocumentId,
            String temporaryStorageKey,
            String fileName,
            long fileSize
    ) {
    }
}
