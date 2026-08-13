package com.group3.vitamins.bidding.bidreview.application.port;

import java.util.List;

// Worker에게 넘길 "보유 역량 현황" 집계 - 개인 식별 정보(이름·사번) 없이 인원수만 제공한다.
// ⚠️ 전공×학력·전공×부서처럼 교차 집계하지 않는다 - 교차할수록 인원수가 줄어 개인 특정 위험이 커진다
// (2026-08-13 결정, 개인정보 보호 목적). 세 집계는 항상 독립적으로 낸다.
public interface BidReviewQualificationPort {

    // 재직 중(퇴사·삭제 제외) · 시스템 계정 제외 · 전공별 인원수.
    List<NameCount> summarizeMajors(Long companyId);

    // 재직 중(퇴사·삭제 제외) · 시스템 계정 제외 · 학위(BACHELOR/MASTER/DOCTOR)별 인원수.
    List<NameCount> summarizeDegrees(Long companyId);

    // 재직 중(퇴사·삭제 제외) · 시스템 계정 제외 · 자격증별 인원수.
    List<NameCount> summarizeCertificates(Long companyId);

    record NameCount(String name, long headcount) {
    }
}
