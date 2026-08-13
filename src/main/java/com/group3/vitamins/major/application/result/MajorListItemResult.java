package com.group3.vitamins.major.application.result;

/** 전공 목록 항목 결과 — 전공 + 사용 사원 수(MAJ-003) + 삭제 가능 여부(파생). */
public record MajorListItemResult(Long majorId, String name, int employeeCount, boolean deletable) {

    public static MajorListItemResult from(MajorListProjection p) {
        return new MajorListItemResult(p.majorId(), p.name(), p.employeeCount(), p.employeeCount() == 0);
    }
}
