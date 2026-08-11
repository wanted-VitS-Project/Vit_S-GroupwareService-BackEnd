package com.group3.vitamins.account.application;

import com.group3.vitamins.account.application.command.ResetPasswordsCommand;
import com.group3.vitamins.account.application.policy.AccountAdminPolicy;
import com.group3.vitamins.account.application.port.AccountQueryPort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.account.application.result.AccountTargetRow;
import com.group3.vitamins.account.application.result.PasswordResetResult;
import com.group3.vitamins.account.application.service.AccountPasswordResetService;
import com.group3.vitamins.account.application.service.AccountPasswordUpdater;
import com.group3.vitamins.account.domain.PasswordResetFailureReason;
import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.account.application.port.MailDeliveryException;
import com.group3.vitamins.account.application.port.PasswordResetMailPort;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AccountPasswordResetService 비밀번호 재설정")
class AccountPasswordResetServiceTest {

    private AccountQueryPort accountQueryPort;
    private AccountPasswordUpdater accountPasswordUpdater;
    private PasswordResetMailPort mailSender;
    private AccountPasswordResetService service;

    @BeforeEach
    void setUp() {
        accountQueryPort = Mockito.mock(AccountQueryPort.class);
        accountPasswordUpdater = Mockito.mock(AccountPasswordUpdater.class);
        mailSender = Mockito.mock(PasswordResetMailPort.class);
        // 아래 둘은 setUp 에서 스텁만 하고 테스트 본문에서 참조하지 않으므로 지역 변수로 둔다.
        ThrottledPasswordEncoder passwordEncoder = Mockito.mock(ThrottledPasswordEncoder.class);
        TempPasswordGenerator tempPasswordGenerator = Mockito.mock(TempPasswordGenerator.class);
        CurrentCompanyIdProvider currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(1L);
        service = new AccountPasswordResetService(
                accountQueryPort, accountPasswordUpdater, passwordEncoder, tempPasswordGenerator,
                mailSender, new AccountAdminPolicy(), currentCompanyIdProvider);

        when(tempPasswordGenerator.generate()).thenReturn("TempPw12!@");
        when(passwordEncoder.encodeBulk(anyString())).thenReturn("$argon2id$temp");
    }

    private AccountTargetRow row(String userId, String email, String role) {
        return new AccountTargetRow(userId, "이름" + userId, email, role, "ADMIN".equals(role));
    }

    private ResetPasswordsCommand command(String actorRole, List<String> userIds) {
        return new ResetPasswordsCommand(actorRole, userIds);
    }

    @Test
    @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> service.resetPasswords(command("MASTER", List.of("EMP001"))))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
        verify(accountQueryPort, never()).findTargets(any(), eq(1L));
    }

    @Test
    @DisplayName("userIds 가 비어 있으면 ACC_INVALID_REQUEST")
    void rejectsEmptyUserIds() {
        assertThatThrownBy(() -> service.resetPasswords(command("ADMIN", List.of())))
                .satisfies(hasCode(AccountErrorCode.ACC_INVALID_REQUEST));
    }

    @Test
    @DisplayName("userIds 가 null 이어도 ACC_INVALID_REQUEST")
    void rejectsNullUserIds() {
        assertThatThrownBy(() -> service.resetPasswords(command("ADMIN", null)))
                .satisfies(hasCode(AccountErrorCode.ACC_INVALID_REQUEST));
    }

    @Test
    @DisplayName("존재하지 않는 사번이 섞이면 전체 거부 — ACC_NOT_FOUND")
    void rejectsWhenAnyMissing() {
        when(accountQueryPort.findTargets(any(), eq(1L)))
                .thenReturn(List.of(row("EMP001", "a@vit.com", "MEMBER")));   // 2개 요청, 1개만 존재

        assertThatThrownBy(() -> service.resetPasswords(command("ADMIN", List.of("EMP001", "EMP999"))))
                .satisfies(hasCode(AccountErrorCode.ACC_NOT_FOUND));
        verify(accountPasswordUpdater, never()).applyResets(any());
    }

    @Test
    @DisplayName("대상에 ADMIN 계정이 있으면 ACC_ADMIN_ACCOUNT_NOT_ALLOWED")
    void rejectsWhenAdminIncluded() {
        when(accountQueryPort.findTargets(any(), eq(1L)))
                .thenReturn(List.of(row("EMP001", "a@vit.com", "MEMBER"), row("ADMIN01", null, "ADMIN")));

        assertThatThrownBy(() -> service.resetPasswords(command("ADMIN", List.of("EMP001", "ADMIN01"))))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_ACCOUNT_NOT_ALLOWED));
        verify(accountPasswordUpdater, never()).applyResets(any());
    }

    @Test
    @SuppressWarnings("unchecked")   // ArgumentCaptor.forClass(Map.class) 는 제네릭을 못 잡는다 (Mockito 한계)
    @DisplayName("이메일 없는 대상은 비밀번호를 바꾸지 않는다 — EMAIL_NOT_REGISTERED(passwordChanged=false)")
    void skipsPasswordChangeWhenEmailMissing() {
        when(accountQueryPort.findTargets(any(), eq(1L)))
                .thenReturn(List.of(row("EMP001", "a@vit.com", "MEMBER"), row("EMP002", null, "MEMBER")));

        PasswordResetResult result = service.resetPasswords(command("ADMIN", List.of("EMP001", "EMP002")));

        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures()).singleElement().satisfies(f -> {
            assertThat(f.userId()).isEqualTo("EMP002");
            assertThat(f.reason()).isEqualTo(PasswordResetFailureReason.EMAIL_NOT_REGISTERED);
            assertThat(f.reason().isPasswordChanged()).isFalse();
        });

        // 이메일 있는 대상만 해싱·저장한다
        // 대상 조회가 현재 회사(1L) 범위로 이뤄졌는지 — 회사 격리 계약 검증
        verify(accountQueryPort).findTargets(any(), eq(1L));

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(accountPasswordUpdater, times(1)).applyResets(captor.capture());
        assertThat(captor.getValue()).containsOnlyKeys("EMP001");
        verify(mailSender, times(1)).sendTempPassword(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("메일 발송이 실패하면 MAIL_SEND_FAILED(passwordChanged=true) — 비번은 이미 바뀌었다")
    void reportsMailFailureAsPasswordChanged() {
        when(accountQueryPort.findTargets(any(), eq(1L)))
                .thenReturn(List.of(row("EMP001", "a@vit.com", "MEMBER")));
        doThrow(new MailDeliveryException(new RuntimeException("smtp down")))
                .when(mailSender).sendTempPassword(anyString(), anyString(), anyString());

        PasswordResetResult result = service.resetPasswords(command("ADMIN", List.of("EMP001")));

        assertThat(result.successCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures()).singleElement().satisfies(f -> {
            assertThat(f.reason()).isEqualTo(PasswordResetFailureReason.MAIL_SEND_FAILED);
            assertThat(f.reason().isPasswordChanged()).isTrue();
        });
        // 비밀번호 변경은 메일보다 먼저 커밋됐다
        verify(accountPasswordUpdater, times(1)).applyResets(any());
    }

    private Consumer<Throwable> hasCode(AccountErrorCode expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
