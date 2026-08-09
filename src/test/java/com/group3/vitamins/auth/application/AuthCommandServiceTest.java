package com.group3.vitamins.auth.application;

import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.auth.application.command.AgreeTermsCommand;
import com.group3.vitamins.auth.application.command.LoginCommand;
import com.group3.vitamins.auth.application.port.UserProfileQueryPort;
import com.group3.vitamins.auth.application.result.UserProfileRow;
import com.group3.vitamins.auth.application.service.AuthCommandService;
import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.auth.infrastructure.adapter.LoginFailureRecordAdapter;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
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

@DisplayName("AuthCommandService 로그인")
class AuthCommandServiceTest {

    private static final String USER_ID = "EMP001";
    private static final String RAW_PASSWORD = "Vit-S!2026";
    private static final String ENCODED = "$argon2id$stored";
    private static final int LOCK_THRESHOLD = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 3, 9, 0);

    private AccountJpaRepository accountRepository;
    private UserProfileQueryPort userProfileQueryPort;
    private PasswordEncoder passwordEncoder;
    private AuthCommandService authService;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountJpaRepository.class);
        userProfileQueryPort = Mockito.mock(UserProfileQueryPort.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("$argon2id$dummy");

        // 실제 구현을 쓴다 — 목으로 대체하면 "실패를 기록한다" 는 계약이 검증되지 않는다.
        // 단 여기엔 프록시가 없어 REQUIRES_NEW 는 걸리지 않는다. 그건 LoginFailureTransactionTest 가 본다.
        LoginFailureRecordAdapter loginFailureRecorder = new LoginFailureRecordAdapter(accountRepository);

        Clock fixed = Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        authService = new AuthCommandService(accountRepository, userProfileQueryPort, loginFailureRecorder,
                passwordEncoder, fixed, LOCK_THRESHOLD, LOCK_DURATION);
        authService.prepareDummyHash();
    }

    @Test
    @DisplayName("존재하지 않는 사번도 해시를 돌린다 — 응답 시간으로 계정 존재가 새면 안 된다")
    void runsDummyHashWhenAccountNotFound() {
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand(USER_ID, RAW_PASSWORD)))
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

        assertThatThrownBy(() -> authService.login(new LoginCommand(USER_ID, RAW_PASSWORD)))
                .satisfies(hasCode(AuthErrorCode.AUTH_ACCOUNT_LOCKED))
                // 423 이다. 비활성(403)과 합치면 프론트가 "관리자 문의" 와 "잠시 후 재시도" 를 구분 못 한다
                .satisfies(hasStatus(423))
                .satisfies(e -> assertThat(e.getMessage()).contains("09:03"));   // 해제 시각을 메시지에 담는다

        verify(passwordEncoder, never()).matches(any(), anyString());
    }

    @Test
    @DisplayName("비활성 계정은 AUTH_ACCOUNT_INACTIVE")
    void rejectsInactiveAccount() {
        AccountEntity account = account();
        ReflectionTestUtils.setField(account, "status", "INACTIVE");
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(new LoginCommand(USER_ID, RAW_PASSWORD)))
                .satisfies(hasCode(AuthErrorCode.AUTH_ACCOUNT_INACTIVE))
                .satisfies(hasStatus(403));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 실패 횟수가 늘어난다")
    void increasesFailCountOnWrongPassword() {
        AccountEntity account = account();
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));
        when(accountRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand(USER_ID, RAW_PASSWORD)))
                .satisfies(hasCode(AuthErrorCode.AUTH_LOGIN_FAILED));

        assertThat(account.getLoginFailCount()).isEqualTo(1);
        assertThat(account.getLockedUntil()).isNull();
        // 실패 기록은 잠금 조회(FOR UPDATE)를 거쳐야 한다. 락 없는 경로로 돌아가면 여기서 잡힌다
        verify(accountRepository).findByUserIdForUpdate(USER_ID);
    }

    @Test
    @DisplayName("임계치에 닿으면 잠기고, 잠금과 함께 실패 횟수가 0으로 돌아간다")
    void locksAccountAtThreshold() {
        AccountEntity account = account();
        ReflectionTestUtils.setField(account, "loginFailCount", LOCK_THRESHOLD - 1);
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));
        when(accountRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand(USER_ID, RAW_PASSWORD)))
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
        when(userProfileQueryPort.findProfile(USER_ID)).thenReturn(Optional.of(profileRow()));

        UserProfileRow profile = authService.login(new LoginCommand(USER_ID, RAW_PASSWORD));

        assertThat(profile.userId()).isEqualTo(USER_ID);
        assertThat(account.getLoginFailCount()).isZero();
        assertThat(account.getLockedUntil()).isNull();
        assertThat(account.getLastLoginAt()).isEqualTo(NOW);
    }

    // ===== 약관 동의 (auth.md §5) =====

    @Test
    @DisplayName("약관 동의 — 동의 시각이 기록되고 게이트가 풀린다")
    void agreeTermsRecordsTimestamp() {
        AccountEntity account = account();   // issue() 는 termsAgreedAt = null (미동의)
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        authService.agreeTerms(new AgreeTermsCommand(USER_ID));

        assertThat(account.hasAgreedTerms()).isTrue();
        assertThat(account.getTermsAgreedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("약관 동의는 멱등 — 이미 동의한 계정이 다시 호출해도 최초 동의 시각을 유지한다")
    void agreeTermsIsIdempotent() {
        AccountEntity account = account();
        LocalDateTime firstAgreedAt = NOW.minusDays(1);
        ReflectionTestUtils.setField(account, "termsAgreedAt", firstAgreedAt);
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(account));

        authService.agreeTerms(new AgreeTermsCommand(USER_ID));

        // NOW 로 덮어쓰지 않고 최초 시각을 유지한다 (감사·법적 기록 보존)
        assertThat(account.getTermsAgreedAt()).isEqualTo(firstAgreedAt);
    }

    @Test
    @DisplayName("약관 동의 — 계정이 없으면 AUTH_UNAUTHENTICATED")
    void agreeTermsRejectsMissingAccount() {
        when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.agreeTerms(new AgreeTermsCommand(USER_ID)))
                .satisfies(hasCode(AuthErrorCode.AUTH_UNAUTHENTICATED));
    }

    @Test
    @DisplayName("termsStatus — 미동의 MEMBER 는 REQUIRED, 동의했으면 AGREED, ADMIN 은 항상 AGREED(스킵)")
    void termsStatusRules() {
        assertThat(profile("MEMBER", null).termsStatus()).isEqualTo("REQUIRED");
        assertThat(profile("MEMBER", NOW).termsStatus()).isEqualTo("AGREED");
        assertThat(profile("ADMIN", null).termsStatus()).isEqualTo("AGREED");   // 공용 계정 스킵
    }

    private UserProfileRow profile(String role, LocalDateTime termsAgreedAt) {
        return new UserProfileRow(USER_ID, "김민준", role, true,
                termsAgreedAt, null, null, "개발팀", "기술본부", "대리", null, null, 1L);
    }

    // ===== 도구 =====

    private AccountEntity account() {
        return AccountEntity.issue(USER_ID, ENCODED, "MEMBER");
    }

    private UserProfileRow profileRow() {
        return new UserProfileRow(USER_ID, "김민준", "MEMBER", true,
                null, null, null, "개발팀", "기술본부", "대리", null, null, 1L);
    }

    private Consumer<Throwable> hasCode(AuthErrorCode expected) {
        return throwable -> assertThat(((DomainException) throwable).getErrorCode()).isEqualTo(expected);
    }

    /** 상태코드는 프론트 분기의 기준이라 에러코드와 함께 고정한다 */
    private Consumer<Throwable> hasStatus(int expected) {
        return throwable -> assertThat(((DomainException) throwable).getHttpStatus()).isEqualTo(expected);
    }
}
