package com.group3.vitamins.auth.application;

import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 로그인 실패 기록을 <b>바깥 트랜잭션과 분리해</b> 커밋한다.
 *
 * <p>🚨 이 클래스가 존재하는 이유는 하나다. {@code AuthService.login()} 은 실패 카운트를 올린 <b>직후</b>
 * {@code UnauthorizedException}(= {@code RuntimeException}) 을 던진다. 같은 트랜잭션에 두면 Spring 의
 * 기본 롤백 규칙이 걸려 <b>올린 카운트와 잠금 시각이 통째로 사라진다</b> — 5회 실패 잠금(`AUTH-003`)이
 * 아예 동작하지 않는다. {@code REQUIRES_NEW} 로 먼저 커밋해야 바깥이 롤백돼도 기록이 남는다.
 *
 * <p>⚠️ 반드시 <b>별도 빈</b>이어야 한다. 같은 클래스 안의 메서드를 부르면 프록시를 안 거쳐
 * {@code REQUIRES_NEW} 가 무시되고 원래 버그로 되돌아간다.
 */
@Component
public class LoginFailureRecorder {

    private final AccountJpaRepository accountRepository;

    public LoginFailureRecorder(AccountJpaRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * 실패 1회를 기록하고 <b>이 트랜잭션 안에서 커밋</b>한다.
     *
     * <p>행을 {@code FOR UPDATE} 로 잠근 뒤 읽고-고치고-쓴다. 동시 요청이 같은 값을 읽어
     * 증가분을 서로 덮어쓰는 것(lost update)을 막는다.
     */
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

    /**
     * 기록 후 상태.
     *
     * <p>호출부(바깥 트랜잭션)가 들고 있는 엔티티는 이 시점에 <b>낡은 값</b>이다. 잠금 여부를
     * 그 엔티티로 다시 판정하면 안 되므로 결과를 값으로 돌려준다.
     *
     * @param lockedUntil 이 실패로 계정이 잠겼으면 해제 시각, 아니면 {@code null}
     */
    public record Result(int failCount, LocalDateTime lockedUntil) {

        public boolean locked() {
            return lockedUntil != null;
        }
    }
}
