package com.group3.vitamins.employee.application.port;

/**
 * 퇴사 처리 시 계정을 비활성화하는 아웃바운드 포트 (`employee.md` §5 · {@code EMP-016}).
 *
 * <p>퇴사는 사원 퇴사일 기록과 계정 {@code INACTIVE} 전환을 <b>한 트랜잭션</b>으로 묶는다 (아키텍처 §2-2).
 * account 의 상태 변경 유스케이스({@code ACC_STATUS_UNCHANGED} 등 별도 규칙 보유)를 재사용하지 않고
 * 어댑터가 {@code AccountEntity.changeStatus("INACTIVE")} 로 직접 전환한다 — 퇴사엔 그 규칙들이 불필요하다.
 */
public interface AccountDeactivationPort {

    /** 해당 사번의 계정을 {@code INACTIVE} 로 바꾼다. */
    void deactivate(String userId);
}
