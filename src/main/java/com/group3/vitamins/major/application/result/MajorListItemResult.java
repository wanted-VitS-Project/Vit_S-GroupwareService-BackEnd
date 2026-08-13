package com.group3.vitamins.major.application.result;

/**
 * 전공 목록 항목 결과 — 전공 + 사용 사원 수(활성, MAJ-003) + 삭제 가능 여부.
 * ⚠️ {@code deletable} 은 활성 수가 아니라 전체 참조 수로 판정한다 — 퇴사·시스템 사원의 학력도 FK 로 삭제를 막기 때문.
 */
public record MajorListItemResult(Long majorId, String name, int employeeCount, boolean deletable) {

    public static MajorListItemResult from(MajorListProjection p) {
        return new MajorListItemResult(p.majorId(), p.name(), p.employeeCount(), p.referenceCount() == 0);
    }
}
