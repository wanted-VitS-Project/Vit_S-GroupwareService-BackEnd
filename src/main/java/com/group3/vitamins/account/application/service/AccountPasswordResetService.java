package com.group3.vitamins.account.application.service;

import com.group3.vitamins.account.application.command.ResetPasswordsCommand;
import com.group3.vitamins.account.application.policy.AccountAdminPolicy;
import com.group3.vitamins.account.application.port.AccountQueryPort;
import com.group3.vitamins.account.application.result.AccountTargetRow;
import com.group3.vitamins.account.application.result.PasswordResetFailureResult;
import com.group3.vitamins.account.application.result.PasswordResetResult;
import com.group3.vitamins.account.application.usecase.AccountPasswordResetUseCase;
import com.group3.vitamins.account.domain.PasswordResetFailureReason;
import com.group3.vitamins.account.domain.TempPasswordGenerator;
import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.account.application.port.MailDeliveryException;
import com.group3.vitamins.account.application.port.PasswordResetMailPort;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.infrastructure.config.security.ThrottledPasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 비밀번호 일괄 재설정 유스케이스 (`.ai/api/account.md` §3).
 *
 * <p><b>트랜잭션을 클래스 전체에 걸지 않는다.</b> 처리 순서 자체가 트랜잭션 경계를 나눈다:
 * <ol>
 *   <li>사전 검증 — 없는 사번·ADMIN 이 섞이면 <b>전체 거부</b> (부분 처리는 원인을 숨긴다)</li>
 *   <li>임시 비밀번호 생성 + 해싱 — <b>트랜잭션 밖</b> (64MB 해시로 DB 커넥션을 오래 잡지 않는다)</li>
 *   <li>DB 반영 — {@link AccountPasswordUpdater} 가 한 트랜잭션으로 커밋</li>
 *   <li>메일 발송 — <b>커밋 후</b>. 여기서만 부분 실패를 허용한다 ({@code MAIL_SEND_FAILED})</li>
 * </ol>
 *
 * <p>이메일 미등록 대상은 2·3·4 를 건너뛴다 — 전달 못 할 비밀번호를 바꾸면 로그인 불가가 되기 때문이다
 * ({@code EMAIL_NOT_REGISTERED} · passwordChanged=false).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountPasswordResetService implements AccountPasswordResetUseCase {

    private final AccountQueryPort accountQueryPort;
    private final AccountPasswordUpdater accountPasswordUpdater;
    private final ThrottledPasswordEncoder passwordEncoder;
    private final TempPasswordGenerator tempPasswordGenerator;
    private final PasswordResetMailPort mailSender;
    private final AccountAdminPolicy accountAdminPolicy;

    @Override
    public PasswordResetResult resetPasswords(ResetPasswordsCommand command) {
        accountAdminPolicy.assertAdmin(command.actorRole());

        List<String> distinctUserIds = distinct(command.userIds());
        if (distinctUserIds.isEmpty()) {
            throw new ValidationException(AccountErrorCode.ACC_INVALID_REQUEST);
        }

        List<AccountTargetRow> targets = validateTargets(distinctUserIds);

        List<AccountTargetRow> withEmail = targets.stream().filter(t -> hasText(t.email())).toList();
        List<AccountTargetRow> withoutEmail = targets.stream().filter(t -> !hasText(t.email())).toList();

        // ── 2·3 해싱(트랜잭션 밖) → 일괄 커밋 ──
        Map<String, String> rawByUserId = new LinkedHashMap<>();
        Map<String, String> encodedByUserId = new LinkedHashMap<>();
        for (AccountTargetRow target : withEmail) {
            String rawPassword = tempPasswordGenerator.generate();
            rawByUserId.put(target.userId(), rawPassword);
            encodedByUserId.put(target.userId(), passwordEncoder.encodeBulk(rawPassword));
        }
        if (!encodedByUserId.isEmpty()) {
            accountPasswordUpdater.applyResets(encodedByUserId);
        }

        // ── 4 메일 발송(커밋 후) ──
        List<PasswordResetFailureResult> failures = new ArrayList<>();
        int successCount = 0;
        for (AccountTargetRow target : withEmail) {
            try {
                mailSender.sendTempPassword(target.email(), target.name(), rawByUserId.get(target.userId()));
                successCount++;
            } catch (MailDeliveryException e) {
                // 비밀번호는 이미 바뀌었다 → passwordChanged=true. 반드시 재시도해야 한다.
                failures.add(new PasswordResetFailureResult(
                        target.userId(), target.name(), PasswordResetFailureReason.MAIL_SEND_FAILED));
            }
        }
        for (AccountTargetRow target : withoutEmail) {
            failures.add(new PasswordResetFailureResult(
                    target.userId(), target.name(), PasswordResetFailureReason.EMAIL_NOT_REGISTERED));
        }

        log.info("비밀번호 재설정 — 요청={} 성공={} 실패={}",
                distinctUserIds.size(), successCount, failures.size());
        return new PasswordResetResult(
                distinctUserIds.size(), successCount, failures.size(), failures);
    }

    /**
     * 존재·ADMIN 여부를 한 번에 검증한다. 어긋나면 <b>전체 거부</b>다.
     *
     * <p>존재하지 않는 사번은 화면 목록에서 선택하므로 올 수 없는 값이고, 부분 처리하면 원인을 숨긴다.
     */
    private List<AccountTargetRow> validateTargets(List<String> distinctUserIds) {
        List<AccountTargetRow> targets = accountQueryPort.findTargets(distinctUserIds);
        if (targets.size() != distinctUserIds.size()) {
            throw new NotFoundException(AccountErrorCode.ACC_NOT_FOUND);
        }
        boolean containsAdmin = targets.stream().anyMatch(t -> "ADMIN".equals(t.role()));
        if (containsAdmin) {
            throw new ForbiddenException(AccountErrorCode.ACC_ADMIN_ACCOUNT_NOT_ALLOWED);
        }
        return targets;
    }

    private List<String> distinct(List<String> userIds) {
        if (userIds == null) {
            return List.of();
        }
        return userIds.stream()
                .filter(Objects::nonNull)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
