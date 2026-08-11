package com.group3.vitamins.bidding.bidnotice.application.port;

import java.time.LocalDateTime;
import java.util.Collection;

public interface CompanyBidNoticeStatePort {

    // 수집 실행에서 확인한 공고를 회사별 상태에 신규 등록하거나 마지막 확인 정보로 갱신합니다.
    void observeAll(
            Long companyId,
            Collection<Long> bidNoticeIds,
            Long runId,
            LocalDateTime observedAt
    );
}
