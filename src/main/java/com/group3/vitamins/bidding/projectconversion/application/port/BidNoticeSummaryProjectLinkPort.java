package com.group3.vitamins.bidding.projectconversion.application.port;

import java.util.Optional;

// 공고 프로젝트 전환 3번("summaryId가 있으면 같은 공고·회사의 확정 요약이며 아직 프로젝트에
// 연결되지 않았는지 확인") 및 9번(project_id 쓰기)을 위한 포트.
// ⚠️ bidsummary 도메인 리포지토리를 직접 주입하지 않는다 - 소비자가 자기 포트를 갖는 이 코드베이스의
// 기존 관례를 따른다(BidNoticeProjectAccessPort·BidReviewProjectLinkPort와 동일 결정).
public interface BidNoticeSummaryProjectLinkPort {

    // 조회 자체를 companyId·noticeId로 스코프한다 - 다른 회사·다른 공고 소속이면 그냥 못 찾은 것으로
    // 취급한다(bidsummary 자체 조회 API들의 기존 관례와 동일 - 별도의 403을 안 만듦).
    Optional<SummarySnapshot> findSummary(Long companyId, Long noticeId, Long summaryId);

    record SummarySnapshot(
            Long summaryId,
            boolean confirmed,
            Long projectId
    ) {
    }
}
