package com.group3.vitamins.bidding.projectconversion.application.port;

// 공고 프로젝트 전환 1단계 - "공고 존재 여부와 현재 회사의 접근 권한 확인" 전용 포트.
// ⚠️ bidreview.BidReviewNoticeDocumentPort와 판정 기준이 같지만(회사 소유 + 미삭제 + DISMISSED 제외),
// 각 sub-feature가 자기 포트를 갖는 이 코드베이스의 기존 관례를 따라 별도로 둔다.
// DISMISSED 공고는 접근 불가로 처리한다(2026-08-13 결정 - 회사가 이미 제외한 공고를 프로젝트로
// 전환하는 건 자연스럽지 않고, bidreview의 검토 생성 접근 판정과도 일관성을 맞춘다).
public interface BidNoticeProjectAccessPort {

    boolean isAccessible(Long companyId, Long noticeId);
}
