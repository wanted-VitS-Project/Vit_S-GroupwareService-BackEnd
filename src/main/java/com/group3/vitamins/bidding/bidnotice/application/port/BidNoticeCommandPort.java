package com.group3.vitamins.bidding.bidnotice.application.port;

import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNotice;

import java.util.Optional;

// 직접 등록 공고의 조회, 중복 확인, 저장에 필요한 영속성 기능을 묶은 포트입니다.
public interface BidNoticeCommandPort {

    // 논리 삭제되지 않은 직접 등록 수집처의 ID를 조회합니다.
    Optional<Long> findManualSourceId();

    // 현재 회사가 소유한 직접 등록 공고를 수정 목적으로 조회합니다.
    Optional<ManualBidNotice> findOwnedManualNotice(Long companyId, Long noticeId);

    // 공용 외부 수집 공고인지 확인하여 직접 등록 공고와 다른 수정 오류를 반환할 수 있게 합니다.
    boolean existsExternalNotice(Long noticeId);

    // 현재 회사를 기준으로 같은 중복 키를 가진 다른 활성 공고가 있는지 확인합니다.
    boolean existsActiveDuplicate(
            Long companyId,
            String manualDedupKey,
            Long excludedNoticeId
    );

    // 직접 등록 공고와 첨부 링크를 하나의 저장 책임으로 반영합니다.
    ManualBidNotice save(ManualBidNotice notice);
}
