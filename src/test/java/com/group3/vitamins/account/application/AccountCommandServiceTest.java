package com.group3.vitamins.account.application;

import com.group3.vitamins.account.application.command.ChangeRoleCommand;
import com.group3.vitamins.account.application.command.ChangeStatusCommand;
import com.group3.vitamins.account.application.policy.AccountAdminPolicy;
import com.group3.vitamins.account.application.port.AccountQueryPort;
import com.group3.vitamins.account.application.result.AccountTargetRow;
import com.group3.vitamins.account.application.service.AccountCommandService;
import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.employee.contract.EmployeeParticipationUnavailableEvent;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.infrastructure.session.SessionTerminator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AccountCommandService 권한·상태 변경")
class AccountCommandServiceTest {

    private static final String ADMIN_ID = "ADMIN01";
    private static final String TARGET_ID = "EMP001";

    private AccountJpaRepository accountRepository;
    private AccountQueryPort accountQueryPort;
    private SessionTerminator sessionTerminator;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private DomainEventPublisher domainEventPublisher;
    private AccountCommandService accountCommandService;

    @BeforeEach
    void setUp() {
        accountRepository = Mockito.mock(AccountJpaRepository.class);
        accountQueryPort = Mockito.mock(AccountQueryPort.class);
        sessionTerminator = Mockito.mock(SessionTerminator.class);
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        domainEventPublisher = Mockito.mock(DomainEventPublisher.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(1L);
        // ADMIN 판정은 순수 컴포넌트라 실제 인스턴스를 그대로 쓴다.
        accountCommandService = new AccountCommandService(
                accountRepository, accountQueryPort, sessionTerminator, new AccountAdminPolicy(),
                currentCompanyIdProvider, domainEventPublisher);
    }

    /** 존재하고 시스템 계정이 아닌 정상 대상(MEMBER · ACTIVE)을 세팅한다. */
    private AccountEntity givenNormalTarget() {
        AccountEntity entity = AccountEntity.issue(TARGET_ID, "$argon2id$stored", "MEMBER");
        when(accountQueryPort.findTarget(TARGET_ID, 1L))
                .thenReturn(Optional.of(new AccountTargetRow(TARGET_ID, "홍길동", "hong@vit.com", "MEMBER", false)));
        when(accountRepository.findByUserId(TARGET_ID)).thenReturn(Optional.of(entity));
        return entity;
    }

    @Nested
    @DisplayName("전역 권한 변경")
    class ChangeRole {

        @Test
        @DisplayName("MASTER 로 변경하면 role 이 바뀌고 대상 세션이 즉시 종료된다")
        void changesRoleAndTerminatesSession() {
            AccountEntity target = givenNormalTarget();

            accountCommandService.changeRole(new ChangeRoleCommand(ADMIN_ID, "ADMIN", TARGET_ID, "MASTER"));

            assertThat(target.getRole()).isEqualTo("MASTER");
            verify(sessionTerminator, times(1)).terminateAll(TARGET_ID);
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 해시·조회 이전에 막는다")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> accountCommandService.changeRole(
                    new ChangeRoleCommand(ADMIN_ID, "MASTER", TARGET_ID, "MEMBER")))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(accountQueryPort, never()).findTarget(anyString(), anyLong());
        }

        @Test
        @DisplayName("ADMIN 권한을 부여하려 하면 ACC_ADMIN_ROLE_NOT_ALLOWED")
        void rejectsAdminRole() {
            assertThatThrownBy(() -> accountCommandService.changeRole(
                    new ChangeRoleCommand(ADMIN_ID, "ADMIN", TARGET_ID, "ADMIN")))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_ROLE_NOT_ALLOWED));
        }

        @Test
        @DisplayName("허용되지 않는 값이면 ACC_INVALID_ROLE")
        void rejectsInvalidRole() {
            assertThatThrownBy(() -> accountCommandService.changeRole(
                    new ChangeRoleCommand(ADMIN_ID, "ADMIN", TARGET_ID, "SUPERUSER")))
                    .satisfies(hasCode(AccountErrorCode.ACC_INVALID_ROLE));
        }

        @Test
        @DisplayName("자기 자신의 권한은 바꿀 수 없다 — ACC_SELF_MODIFICATION_NOT_ALLOWED")
        void rejectsSelfModification() {
            assertThatThrownBy(() -> accountCommandService.changeRole(
                    new ChangeRoleCommand(ADMIN_ID, "ADMIN", ADMIN_ID, "MASTER")))
                    .satisfies(hasCode(AccountErrorCode.ACC_SELF_MODIFICATION_NOT_ALLOWED));
        }

        @Test
        @DisplayName("계정이 없으면 ACC_NOT_FOUND")
        void rejectsMissingAccount() {
            when(accountQueryPort.findTarget(TARGET_ID, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountCommandService.changeRole(
                    new ChangeRoleCommand(ADMIN_ID, "ADMIN", TARGET_ID, "MASTER")))
                    .satisfies(hasCode(AccountErrorCode.ACC_NOT_FOUND));
        }

        @Test
        @DisplayName("시스템 계정은 대상이 될 수 없다 — ACC_SYSTEM_ACCOUNT_NOT_ALLOWED")
        void rejectsSystemAccount() {
            when(accountQueryPort.findTarget(TARGET_ID, 1L))
                    .thenReturn(Optional.of(new AccountTargetRow(TARGET_ID, "시스템", null, "ADMIN", true)));

            assertThatThrownBy(() -> accountCommandService.changeRole(
                    new ChangeRoleCommand(ADMIN_ID, "ADMIN", TARGET_ID, "MASTER")))
                    .satisfies(hasCode(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED));
        }
    }

    @Nested
    @DisplayName("계정 상태 변경")
    class ChangeStatus {

        @Test
        @DisplayName("INACTIVE 로 바꾸면 상태가 바뀌고 세션이 종료된다")
        void deactivatesAndTerminatesSession() {
            AccountEntity target = givenNormalTarget();   // issue() 는 ACTIVE

            accountCommandService.changeStatus(new ChangeStatusCommand("ADMIN", TARGET_ID, "INACTIVE"));

            assertThat(target.getStatus()).isEqualTo("INACTIVE");
            verify(domainEventPublisher).publish(
                    new EmployeeParticipationUnavailableEvent(TARGET_ID, 1L));
            verify(sessionTerminator, times(1)).terminateAll(TARGET_ID);
        }

        @Test
        @DisplayName("이미 같은 상태면 ACC_STATUS_UNCHANGED — 세션은 건드리지 않는다")
        void rejectsUnchangedStatus() {
            givenNormalTarget();   // ACTIVE

            assertThatThrownBy(() -> accountCommandService.changeStatus(
                    new ChangeStatusCommand("ADMIN", TARGET_ID, "ACTIVE")))
                    .satisfies(hasCode(AccountErrorCode.ACC_STATUS_UNCHANGED));
            verify(sessionTerminator, never()).terminateAll(anyString());
        }

        @Test
        @DisplayName("허용되지 않는 값이면 ACC_INVALID_STATUS")
        void rejectsInvalidStatus() {
            assertThatThrownBy(() -> accountCommandService.changeStatus(
                    new ChangeStatusCommand("ADMIN", TARGET_ID, "PAUSED")))
                    .satisfies(hasCode(AccountErrorCode.ACC_INVALID_STATUS));
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> accountCommandService.changeStatus(
                    new ChangeStatusCommand("MEMBER", TARGET_ID, "INACTIVE")))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
        }
    }

    private Consumer<Throwable> hasCode(AccountErrorCode expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
