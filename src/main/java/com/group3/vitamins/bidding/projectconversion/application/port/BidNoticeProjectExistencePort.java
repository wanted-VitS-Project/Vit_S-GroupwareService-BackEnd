package com.group3.vitamins.bidding.projectconversion.application.port;

// 4번("기존 프로젝트 전환 여부 확인") 전용 포트. 최종 방어선은 project 도메인의
// DB UNIQUE(project.bid_notice_id, company_id) + ProjectCommandService.checkBidNoticeNotLinked이고,
// 여기서는 이미 전환된 공고를 뒤이은 5~6번 검증(권한·초대자 확인)까지 낭비하지 않고 빨리 걸러내기 위한
// 선확인이다 - 동시 요청 경합은 여전히 project 도메인의 UNIQUE 제약이 최종적으로 막는다.
public interface BidNoticeProjectExistencePort {

    boolean existsForNotice(Long companyId, Long noticeId);
}
