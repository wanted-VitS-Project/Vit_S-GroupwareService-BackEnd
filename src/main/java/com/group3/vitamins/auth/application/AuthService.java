package com.group3.vitamins.auth.application;

import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.auth.domain.PasswordPolicy;
import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.auth.infrastructure.persistence.AuthQueryMapper;
import com.group3.vitamins.auth.infrastructure.persistence.UserProfileRow;
import com.group3.vitamins.global.domain.common.error.exception.AccountLockedException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.UnauthorizedException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 인증 유스케이스 — 로그인 · 내 정보 · 비밀번호 변경.
 *
 * <p>세션 수립은 {@link AuthSessionManager}, 조회는 {@link AuthQueryMapper}(MyBatis),
 * 계정 상태 변경은 {@link AccountEntity}(JPA)가 맡는다.
 */
@Slf4j
@Service
public class AuthService {

    private static final DateTimeFormatter UNLOCK_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final AccountJpaRepository accountRepository;
    private final AuthQueryMapper authQueryMapper;
    private final LoginFailureRecorder loginFailureRecorder;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final int lockThreshold;
    private final Duration lockDuration;

    /**
     * 존재하지 않는 사번에도 해시를 돌리기 위한 더미.
     *
     * <p>계정이 없다고 즉시 401 을 주면 <b>응답 시간 차이(0.4초 vs 1ms)로 사번 존재 여부가 샌다</b>
     * (`AUTH-003` 계정 열거 방어). 기동 시 1회 생성한다.
     */
    private String dummyHash;

    public AuthService(AccountJpaRepository accountRepository,
                       AuthQueryMapper authQueryMapper,
                       LoginFailureRecorder loginFailureRecorder,
                       PasswordEncoder passwordEncoder,
                       Clock clock,
                       @Value("${security.login.lock-threshold}") int lockThreshold,
                       @Value("${security.login.lock-duration}") Duration lockDuration) {
        this.accountRepository = accountRepository;
        this.authQueryMapper = authQueryMapper;
        this.loginFailureRecorder = loginFailureRecorder;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.lockThreshold = lockThreshold;
        this.lockDuration = lockDuration;
    }

    @PostConstruct
    void prepareDummyHash() {
        this.dummyHash = passwordEncoder.encode("vitamins-dummy-" + System.nanoTime());
    }

    /**
     * 로그인.
     *
     * <p><b>검사 순서에 의도가 있다.</b> 잠금·비활성 확인이 비밀번호 검증보다 <b>앞</b>이다 —
     * 잠긴 계정에까지 64MB 짜리 해시를 돌리면 잠금이 오히려 DoS 증폭 수단이 된다.
     *
     * <p>이 순서는 "사번이 존재한다" 는 사실을 노출한다. 다만 <b>계정 잠금 기능 자체가 존재를 노출</b>하므로
     * (없는 사번은 잠기지 않는다) 순서를 바꿔도 막을 수 없다. 열거 속도는 IP 레이트리밋으로 제한한다.
     */
    @Transactional
    public UserProfileRow login(String userId, String rawPassword) {
        Optional<AccountEntity> found = accountRepository.findByUserId(userId);

        if (found.isEmpty()) {
            passwordEncoder.matches(rawPassword, dummyHash);   // 타이밍 맞추기 — 결과는 버린다
            throw new UnauthorizedException(AuthErrorCode.AUTH_LOGIN_FAILED);
        }

        AccountEntity account = found.get();
        LocalDateTime now = LocalDateTime.now(clock);

        if (account.isLocked(now)) {
            throw lockedException(account.getLockedUntil());
        }
        if (account.isInactive()) {
            throw new ForbiddenException(AuthErrorCode.AUTH_ACCOUNT_INACTIVE);
        }

        if (!passwordEncoder.matches(rawPassword, account.getPassword())) {
            // 🚨 이 트랜잭션에서 기록하면 안 된다. 바로 아래 예외로 전부 롤백돼 잠금이 걸리지 않는다.
            //    별도 트랜잭션(REQUIRES_NEW)에서 먼저 커밋시킨다.
            LoginFailureRecorder.Result failure =
                    loginFailureRecorder.record(userId, lockThreshold, now, now.plus(lockDuration));
            // ⚠️ 사번은 개인 식별자다. 로그 수집 시스템에 평문으로 쌓이면 안 된다.
            //    공격 분석에는 내부 키로 충분하다.
            log.info("로그인 실패 — accountId={} failCount={}", account.getAccountId(), failure.failCount());
            if (failure.locked()) {
                throw lockedException(failure.lockedUntil());
            }
            throw new UnauthorizedException(AuthErrorCode.AUTH_LOGIN_FAILED);
        }

        account.recordLoginSuccess(now);

        // ⚠️ 아래 조회는 MyBatis 라 위 JPA 변경(last_login_at)이 아직 안 보인다 (flush 전).
        //    로그인 응답에 lastLoginAt 이 없어서 지금은 문제가 없다.
        //    응답에 추가하게 되면 여기서 entityManager.flush() 를 먼저 호출해야 한다.
        return loadProfile(userId);
    }

    @Transactional(readOnly = true)
    public UserProfileRow loadProfile(String userId) {
        return authQueryMapper.findProfile(userId)
                // 세션은 살아 있는데 계정이 사라진 경우. 재로그인시키는 게 맞다.
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.AUTH_UNAUTHENTICATED));
    }

    /**
     * 비밀번호 변경.
     *
     * <p>{@code currentPassword} 는 최초 변경(`RESET_REQUIRED`)일 때만 생략할 수 있다.
     * 이미 임시 비밀번호로 인증해 세션이 있기 때문이다 (`.ai/api/auth.md` §4).
     */
    @Transactional
    public void changePassword(String userId, String currentPassword,
                               String newPassword, String newPasswordConfirm) {
        AccountEntity account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.AUTH_UNAUTHENTICATED));

        boolean initialChange = account.isMustChangePassword();
        if (!initialChange) {
            if (isBlank(currentPassword)) {
                throw new ValidationException(AuthErrorCode.AUTH_CURRENT_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(currentPassword, account.getPassword())) {
                throw new ValidationException(AuthErrorCode.AUTH_CURRENT_PASSWORD_INVALID);
            }
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            throw new ValidationException(AuthErrorCode.AUTH_PASSWORD_CONFIRM_MISMATCH);
        }
        PasswordPolicy.validate(newPassword);

        // ⚠️ 해시 호출 횟수를 줄인다. permit 이 2뿐이라 요청 1건이 슬롯을 3회 점유하면
        //    비밀번호 변경이 몰릴 때 로그인까지 503 이 난다.
        //    일반 변경은 바로 위에서 currentPassword 가 저장 해시와 일치함을 이미 확인했으므로,
        //    재사용 여부는 문자열 비교로 판정할 수 있다. 최초 변경 경로는 비교 대상이 없어 해시가 필요하다.
        boolean reusingCurrent = initialChange
                ? passwordEncoder.matches(newPassword, account.getPassword())
                : newPassword.equals(currentPassword);
        if (reusingCurrent) {
            throw new ValidationException(AuthErrorCode.AUTH_PASSWORD_UNCHANGED);
        }

        account.changePassword(passwordEncoder.encode(newPassword));
        log.info("비밀번호 변경 완료 — accountId={}", account.getAccountId());
    }

    /**
     * 명세: 잠금 응답의 {@code message} 에 해제 시각을 담는다. 코드는 그대로 둔다.
     *
     * <p>상태코드는 <b>423</b> 이다 — 비활성(403)과 달리 시간이 지나면 스스로 풀리므로 프론트 분기가 다르다.
     */
    private AccountLockedException lockedException(LocalDateTime lockedUntil) {
        return new AccountLockedException(
                AuthErrorCode.AUTH_ACCOUNT_LOCKED,
                "로그인 실패가 누적되어 계정이 잠겼습니다. %s 이후에 다시 시도해 주세요."
                        .formatted(lockedUntil.format(UNLOCK_TIME_FORMAT)));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
