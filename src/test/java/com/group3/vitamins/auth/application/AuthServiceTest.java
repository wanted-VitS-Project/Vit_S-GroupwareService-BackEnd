package com.group3.vitamins.auth.application;

import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.auth.infrastructure.persistence.AuthQueryMapper;
import com.group3.vitamins.auth.infrastructure.persistence.UserProfileRow;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AuthService 로그인")
class AuthServiceTest {

    private static final String USER_ID = "EMP001";
    private static final String RAW_PASSWORD = "Vit-S!2026";
    private static final String ENCODED = "$argon2id$stored";
    private static final int LOCK_THRESHOLD = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 3, 9, 0);

    private AccountJpaRepository accountRepository;
    private AuthQueryMapper authQueryMapper;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountJpaRepository.class);
        authQueryMapper = Mockito.mock(AuthQueryMapper.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("$argon2id$dummy");

        Clock fixed = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        authService = new AuthService(accountRepository, authQueryMapper, passwordEncoder,
                fixed, LOCK_THRESHOLD, LOCK_DURATION);
        authService.prepareDummyHash();
    }

    @Test
    @DisplayName("존재하지 않는 사번도 해시를 돌린다 — 응답 시간으로 계정 존재가 새면 안 된다")
    void runsDummyHashWhenAccountNotFound() {
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(USER_ID, RAW_PASSWORD))
                .satisfies(hasCode(AuthErrorCode.AUTH_LOGIN_FAILED));

        // 더미 해시로라도 matches 를 호출해 시간을 맞춰야 한다
        verify(passwordEncoder, times(1)).matches(any(), anyString());
    }

    @Test
    @DisplayName("잠긴 계정은 해시를 돌리지 않는다 — 잠금이 DoS 증폭 수단이 되면 안 된다")
    void doesNotHashWhenLocked() {
        AccountEntity account = account();
        ReflectionTestUtils.setField(account, "lockedUntil", NOW.plusMinutes(3));
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(USER_ID, RAW_PASSWORD))
                .satisfies(hasCode(AuthErrorCode.AUTH_ACCOUNT_LOCKED))
                .satisfies(e -> assertThat(e.getMessage()).contains("09:03"));   // 해제 시각을 메시지에 담는다

        verify(passwordEncoder, never()).matches(any(), anyString());
    }

    @Test
    @DisplayName("비활성 계정은 AUTH_ACCOUNT_INACTIVE")
    void rejectsInactiveAccount() {
        AccountEntity account = account();
        ReflectionTestUtils.setField(account, "status", "INACTIVE");
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(USER_ID, RAW_PASSWORD))
                .satisfies(hasCode(AuthErrorCode.AUTH_ACCOUNT_INACTIVE));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 실패 횟수가 늘어난다")
    void increasesFailCountOnWrongPassword() {
        AccountEntity account = account();
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(USER_ID, RAW_PASSWORD))
                .satisfies(hasCode(AuthErrorCode.AUTH_LOGIN_FAILED));

        assertThat(account.getLoginFailCount()).isEqualTo(1);
        assertThat(account.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("임계치에 닿으면 잠기고, 잠금과 함께 실패 횟수가 0으로 돌아간다")
    void locksAccountAtThreshold() {
        AccountEntity account = account();
        ReflectionTestUtils.setField(account, "loginFailCount", LOCK_THRESHOLD - 1);
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(USER_ID, RAW_PASSWORD))
                .satisfies(hasCode(AuthErrorCode.AUTH_ACCOUNT_LOCKED));

        assertThat(account.getLockedUntil()).isEqualTo(NOW.plus(LOCK_DURATION));
        // 0으로 되돌리지 않으면 잠금 해제 직후 1회만 틀려도 다시 잠긴다
        assertThat(account.getLoginFailCount()).isZero();
    }

    @Test
    @DisplayName("성공하면 실패 횟수·잠금이 풀리고 마지막 로그인 시각이 기록된다")
    void resetsStateOnSuccess() {
        AccountEntity account = account();
        ReflectionTestUtils.setField(account, "loginFailCount", 3);
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED)).thenReturn(true);
        when(authQueryMapper.findProfile(USER_ID)).thenReturn(Optional.of(profileRow()));

        UserProfileRow profile = authService.login(USER_ID, RAW_PASSWORD);

        assertThat(profile.userId()).isEqualTo(USER_ID);
        assertThat(account.getLoginFailCount()).isZero();
        assertThat(account.getLockedUntil()).isNull();
        assertThat(account.getLastLoginAt()).isEqualTo(NOW);
    }

    // ===== 도구 =====

    private AccountEntity account() {
        return AccountEntity.issue(USER_ID, ENCODED, "MEMBER");
    }

    private UserProfileRow profileRow() {
        return new UserProfileRow(USER_ID, "김민준", "MEMBER", true,
                null, null, "개발팀", "기술본부", "대리", null, null);
    }

    private Consumer<Throwable> hasCode(AuthErrorCode expected) {
        return throwable -> assertThat(((DomainException) throwable).getErrorCode()).isEqualTo(expected);
    }
}
