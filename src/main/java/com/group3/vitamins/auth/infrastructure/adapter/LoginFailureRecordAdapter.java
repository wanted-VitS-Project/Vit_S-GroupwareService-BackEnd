package com.group3.vitamins.auth.infrastructure.adapter;

import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.auth.application.port.LoginFailureRecordPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * {@link LoginFailureRecordPort} 의 구현 — 로그인 실패 기록을 <b>바깥 트랜잭션과 분리해</b> 커밋한다.
 *
 * <p>🚨 {@code AuthCommandService.login()} 은 실패 카운트를 올린 직후 예외를 던진다. 같은 트랜잭션에 두면
 * 기본 롤백 규칙이 걸려 <b>올린 카운트와 잠금 시각이 통째로 사라진다</b> — 5회 실패 잠금(`AUTH-003`)이
 * 아예 동작하지 않는다. {@code REQUIRES_NEW} 로 먼저 커밋해야 바깥이 롤백돼도 기록이 남는다.
 *
 * <p>⚠️ 공유 인증 엔티티 {@link AccountEntity} 를 직접 쓴다(B2 — account 도메인 소유 테이블에 대한 auth 의 쓰기).
 * <b>반드시 별도 빈</b>이어야 프록시를 거쳐 {@code REQUIRES_NEW} 가 걸린다 — 같은 클래스 안 자기 호출이면 무시된다.
 */
@Component
public class LoginFailureRecordAdapter implements LoginFailureRecordPort {

    private final AccountJpaRepository accountRepository;

    public LoginFailureRecordAdapter(AccountJpaRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * 실패 1회를 기록하고 <b>이 트랜잭션 안에서 커밋</b>한다.
     * 행을 {@code FOR UPDATE} 로 잠근 뒤 읽고-고치고-쓴다(동시 요청의 lost update 방지).
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result record(String userId, int threshold, LocalDateTime now, LocalDateTime lockUntil) {
        Optional<AccountEntity> found = accountRepository.findByUserIdForUpdate(userId);
        if (found.isEmpty()) {
            // 조회와 이 시점 사이에 계정이 지워진 경우. 기록할 대상이 없으니 그냥 실패로 끝낸다.
            return new Result(0, null);
        }

        AccountEntity account = found.get();
        account.recordLoginFailure(threshold, lockUntil);
        return new Result(
                account.getLoginFailCount(),
                account.isLocked(now) ? account.getLockedUntil() : null);
    }
}
