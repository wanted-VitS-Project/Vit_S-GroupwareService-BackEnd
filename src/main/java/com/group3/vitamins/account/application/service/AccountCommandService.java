package com.group3.vitamins.account.application.service;

import com.group3.vitamins.account.application.command.ChangeRoleCommand;
import com.group3.vitamins.account.application.command.ChangeStatusCommand;
import com.group3.vitamins.account.application.policy.AccountAdminPolicy;
import com.group3.vitamins.account.application.port.AccountQueryPort;
import com.group3.vitamins.account.application.result.AccountTargetRow;
import com.group3.vitamins.account.application.usecase.AccountCommandUseCase;
import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.employee.contract.EmployeeParticipationUnavailableEvent;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.infrastructure.session.SessionTerminator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

/**
 * 계정 관리 유스케이스 — 전역 권한 변경 · 계정 상태 변경 (`.ai/api/account.md` §1·§2).
 *
 * <p>두 API 모두 <b>ADMIN 전용</b>이다. ADMIN 판정은 {@link AccountAdminPolicy} 가 도메인 코드
 * ({@code ACC_ADMIN_REQUIRED})와 함께 한다.
 *
 * <p>계정 존재·시스템계정 여부는 {@code employee} 조인이 필요해 {@link AccountQueryPort}(MyBatis)로 먼저 보고,
 * 실제 변경은 공유 인증 엔티티 {@link AccountEntity}(JPA)로 한다. 비밀번호 재설정(§3)은
 * {@code AccountPasswordResetService} 로 분리했다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCommandService implements AccountCommandUseCase {

    /** 이 API 로 부여 가능한 role. {@code ADMIN} 은 개발자가 직접 발급하므로 제외된다 (`ACC-023`) */
    private static final Set<String> ASSIGNABLE_ROLES = Set.of("MASTER", "MEMBER");
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final String ADMIN = "ADMIN";
    private static final String INACTIVE = "INACTIVE";

    private final AccountJpaRepository accountRepository;
    private final AccountQueryPort accountQueryPort;
    private final SessionTerminator sessionTerminator;
    private final AccountAdminPolicy accountAdminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 전역 권한 변경 (`.ai/api/account.md` §1).
     *
     * <p>role 은 세션의 권한(authorities)에 실려 있으므로 DB 만 바꾸면 다음 로그인까지 반영되지 않는다.
     * 서버측 세션을 택한 이유가 <b>즉시 반영</b>이므로 대상의 세션을 종료해 재로그인 시 새 role 이 적용되게 한다.
     */
    @Override
    @Transactional
    public void changeRole(ChangeRoleCommand command) {
        accountAdminPolicy.assertAdmin(command.actorRole());
        validateAssignableRole(command.role());
        if (command.targetUserId().equals(command.actorUserId())) {
            throw new ValidationException(AccountErrorCode.ACC_SELF_MODIFICATION_NOT_ALLOWED);
        }

        AccountEntity account = loadModifiableTarget(command.targetUserId());
        account.changeRole(command.role());
        terminateSessionsAfterCommit(command.targetUserId());
        log.info("전역 권한 변경 — targetUserId={} role={}", command.targetUserId(), command.role());
    }

    /**
     * 계정 상태 변경 (`.ai/api/account.md` §2).
     *
     * <p>비활성화하면 그 사용자의 세션을 즉시 끊는다. 안 끊으면 이미 로그인한 사용자가
     * 유휴 타임아웃(4시간)까지 계속 접근할 수 있어 "비활성화" 가 이름값을 못 한다.
     */
    @Override
    @Transactional
    public void changeStatus(ChangeStatusCommand command) {
        accountAdminPolicy.assertAdmin(command.actorRole());
        validateStatus(command.status());

        AccountEntity account = loadModifiableTarget(command.targetUserId());
        if (command.status().equals(account.getStatus())) {
            throw new ValidationException(AccountErrorCode.ACC_STATUS_UNCHANGED);
        }

        account.changeStatus(command.status());
        if (INACTIVE.equals(command.status())) {
            domainEventPublisher.publish(new EmployeeParticipationUnavailableEvent(
                    command.targetUserId(), currentCompanyIdProvider.currentCompanyId()));
            terminateSessionsAfterCommit(command.targetUserId());
        }
        log.info("계정 상태 변경 — targetUserId={} status={}", command.targetUserId(), command.status());
    }

    // ===== 공통 =====

    /**
     * 세션 종료를 <b>트랜잭션 커밋 이후</b>로 미룬다.
     *
     * <p>커밋 전에 종료하면, 종료와 커밋 사이에 대상이 로그인할 경우 <b>옛 role·status 로 만든 세션이
     * 커밋 뒤에도 살아남는다</b>(종료 대상 조회를 이미 지나갔으므로). afterCommit 에 걸어 이 틈을 없앤다.
     *
     * <p>트랜잭션 동기화가 없는 경우(순수 단위 테스트 등)에는 즉시 종료한다.
     */
    private void terminateSessionsAfterCommit(String userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sessionTerminator.terminateAll(userId);
                }
            });
        } else {
            sessionTerminator.terminateAll(userId);
        }
    }

    /**
     * 대상 계정을 존재·시스템계정 검증 후 JPA 로 로드한다.
     *
     * <p>존재·{@code is_system} 은 조인이 필요해 MyBatis 로 먼저 보고, 실제 변경은 JPA 엔티티로 한다.
     */
    private AccountEntity loadModifiableTarget(String targetUserId) {
        AccountTargetRow target = accountQueryPort
                .findTarget(targetUserId, currentCompanyIdProvider.currentCompanyId())
                .orElseThrow(() -> new NotFoundException(AccountErrorCode.ACC_NOT_FOUND));
        if (target.isSystem()) {
            throw new ForbiddenException(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED);
        }
        // FK 로 account ↔ employee 가 1:1 이므로 위에서 존재를 확인했으면 여기서 비는 일은 없다.
        return accountRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new NotFoundException(AccountErrorCode.ACC_NOT_FOUND));
    }

    private void validateAssignableRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ValidationException(AccountErrorCode.ACC_INVALID_ROLE);
        }
        if (ADMIN.equals(role)) {
            throw new ValidationException(AccountErrorCode.ACC_ADMIN_ROLE_NOT_ALLOWED);
        }
        if (!ASSIGNABLE_ROLES.contains(role)) {
            throw new ValidationException(AccountErrorCode.ACC_INVALID_ROLE);
        }
    }

    private void validateStatus(String status) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new ValidationException(AccountErrorCode.ACC_INVALID_STATUS);
        }
    }
}
