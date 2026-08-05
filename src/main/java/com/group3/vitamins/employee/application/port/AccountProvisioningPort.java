package com.group3.vitamins.employee.application.port;

/**
 * 사원 등록 시 계정을 함께 발급하는 아웃바운드 포트 (`employee.md` §3 · {@code ACC-002}).
 *
 * <p>계정은 별도 애그리게이트(account)지만 사원 없이 존재하지 않으므로 <b>같은 트랜잭션</b>에서 생성한다
 * (아키텍처 §2-2 — 경계 넘는 쓰기). account 에는 "계정 생성" 인바운드 유스케이스가 없어(생성 엔드포인트 없음)
 * 어댑터가 공유 엔티티 {@code AccountEntity.issue()} 로 직접 INSERT 한다.
 *
 * <p>비밀번호 해싱(Argon2 64MB)은 <b>이 포트 밖</b>에서 끝낸다 — 호출자가 트랜잭션 진입 전에 해시해
 * 넘긴다(DB 커넥션을 해시 동안 잡지 않기 위해, 비번 재설정 유스케이스와 동일).
 */
public interface AccountProvisioningPort {

    /**
     * 새 계정을 발급한다. {@code role} 은 {@code MASTER}·{@code MEMBER} (ADMIN 은 호출 전 서비스가 차단),
     * 상태 ACTIVE·초기 비밀번호 변경 필요 플래그는 {@code AccountEntity.issue} 가 세운다.
     *
     * @param encodedPassword 이미 Argon2 로 해시된 초기 비밀번호
     */
    void provision(String userId, String role, String encodedPassword);
}
