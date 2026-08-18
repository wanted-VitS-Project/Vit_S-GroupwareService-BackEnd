package com.group3.vitamins.employee.application.result;

import java.util.List;

/**
 * 엑셀 일괄 등록의 자동 생성 마스터 묶음 — 전공·자격증 (employee.md §7 {@code newMasters} · §8 {@code createdMasters} 가 같은 구조).
 * {@code autoCreateMasters=false} 면 항상 {@link #empty()} 다.
 */
public record PendingMasters(List<PendingMaster> majors, List<PendingMaster> certificates) {

    private static final PendingMasters EMPTY = new PendingMasters(List.of(), List.of());

    public static PendingMasters empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return majors.isEmpty() && certificates.isEmpty();
    }
}
