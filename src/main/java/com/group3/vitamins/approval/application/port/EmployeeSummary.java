package com.group3.vitamins.approval.application.port;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code employee} 라이브 조회 결과 중 결재 도메인이 필요로 하는 최소 정보 (INV-11)
 *
 * @param companyId 소속 회사(테넌트) 번호. 회사 격리 판정용이며 <b>응답에는 나가지 않는다</b> —
 *                  사원을 못 찾아 fallback 으로 만든 경우 {@code null} 이다
 */
public record EmployeeSummary(String userId, String name, String position, String department, String role,
                              Long companyId, String accountStatus, LocalDate resignedAt, LocalDateTime deletedAt) {

    /** 퇴사·논리 삭제·계정 비활성 중 하나라도 해당하면 새 결재 참여 및 기존 결재 처리가 불가능하다. */
    public boolean participationUnavailable() {
        return resignedAt != null || deletedAt != null || !"ACTIVE".equals(accountStatus);
    }
}
