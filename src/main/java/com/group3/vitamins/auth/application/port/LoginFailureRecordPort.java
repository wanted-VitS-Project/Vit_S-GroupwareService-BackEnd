package com.group3.vitamins.auth.application.port;

import java.time.LocalDateTime;

/**
 * 로그인 실패 기록을 <b>바깥 트랜잭션과 분리해</b> 커밋하는 아웃바운드 포트.
 *
 * <p>🚨 이 포트가 존재하는 이유는 하나다. {@code AuthCommandService.login()} 은 실패 카운트를 올린 <b>직후</b>
 * {@code UnauthorizedException}(= {@code RuntimeException}) 을 던진다. 같은 트랜잭션에 두면 Spring 의
 * 기본 롤백 규칙이 걸려 <b>올린 카운트와 잠금 시각이 통째로 사라진다</b> — 5회 실패 잠금(`AUTH-003`)이
 * 아예 동작하지 않는다. 구현({@code LoginFailureRecordAdapter})은 {@code REQUIRES_NEW} 로 먼저 커밋해야
 * 바깥이 롤백돼도 기록이 남는다. <b>반드시 별도 빈</b>이어야 프록시를 거쳐 {@code REQUIRES_NEW} 가 걸린다.
 */
public interface LoginFailureRecordPort {

    /**
     * 실패 1회를 기록하고 <b>별도 트랜잭션에서 커밋</b>한다.
     * 행을 {@code FOR UPDATE} 로 잠근 뒤 읽고-고치고-쓴다(동시 요청의 lost update 방지).
     */
    Result record(String userId, int threshold, LocalDateTime now, LocalDateTime lockUntil);

    /**
     * 기록 후 상태.
     *
     * <p>호출부(바깥 트랜잭션)가 들고 있는 엔티티는 이 시점에 <b>낡은 값</b>이다. 잠금 여부를
     * 그 엔티티로 다시 판정하면 안 되므로 결과를 값으로 돌려준다.
     *
     * @param lockedUntil 이 실패로 계정이 잠겼으면 해제 시각, 아니면 {@code null}
     */
    record Result(int failCount, LocalDateTime lockedUntil) {

        public boolean locked() {
            return lockedUntil != null;
        }
    }
}
