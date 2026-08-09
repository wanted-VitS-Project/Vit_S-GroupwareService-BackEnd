package com.group3.vitamins.settlement.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.settlement.application.command.UpdateSettlementItemCommand;
import com.group3.vitamins.settlement.application.policy.SettlementEligibilityPolicy;
import com.group3.vitamins.settlement.application.usecase.SettlementCommandUseCase;
import com.group3.vitamins.settlement.domain.exception.SettlementErrorCode;
import com.group3.vitamins.settlement.domain.model.Settlement;
import com.group3.vitamins.settlement.domain.model.SettlementProgress;
import com.group3.vitamins.settlement.domain.model.SettlementStatus;
import com.group3.vitamins.settlement.domain.model.SettlementType;
import com.group3.vitamins.settlement.domain.repository.SettlementRepository;
import com.group3.vitamins.settlement.infrastructure.blockdetail.SettlementDetailMapper;
import com.group3.vitamins.settlement.infrastructure.blockdetail.SettlementDetailRow;
import com.group3.vitamins.settlement.infrastructure.security.AccountNumberCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 정산 블록 생성·삭제는 Block 도메인이 처리한다({@link SettlementHandlerService}) — 여기는
 * 정산 항목 작성/수정(PATCH)만 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SettlementCommandService implements SettlementCommandUseCase {

    private final SettlementEligibilityPolicy eligibilityPolicy;
    private final SettlementRepository settlementRepository;
    private final AccountNumberCipher accountNumberCipher;
    private final DomainEventPublisher domainEventPublisher;
    private final SettlementDetailMapper settlementDetailMapper;

    @Override
    public UpdateSettlementItemView upsertItem(UpdateSettlementItemCommand command) {
        log.info("정산 항목 작성/수정 요청 - settleId={}, userId={}", command.settleId(), command.userId());

        SettlementType type = parseType(command.type());
        validateRequiredFields(command);
        if (type == SettlementType.OUTCOME) {
            validateOutcomeAccountInfo(command);
        }

        Settlement before = eligibilityPolicy.getActiveSettlementOrThrow(command.settleId());
        eligibilityPolicy.assertEditPermission(command.settleId(), command.userId(), command.role());

        assertModifiable(before);
        eligibilityPolicy.assertNoTypeDowngrade(before.getType(), type);

        // SETL-008 검증(조회) 전에 같은 프로젝트의 정산 블록 전체를 잠근다 — 두 개의 빈 블록이 동시에
        // PATCH되면서 서로 "기준값 없음"으로 읽고 다른 totalAmount를 저장하는 레이스를 막는다.
        settlementDetailMapper.lockSiblingSettlementBlocksForUpdate(command.settleId());
        assertTotalAmountConsistent(command.settleId(), type, command.totalAmount());

        String encryptedAccountNumber = command.accountNumber() == null
                ? null
                : accountNumberCipher.encrypt(command.accountNumber());

        Settlement saved = settlementRepository.updateItem(
                command.settleId(), type, command.roundNo(), command.totalAmount(),
                command.plannedAmount(), command.plannedTaxAmount(), command.plannedDate(),
                command.traderName(), command.bankName(), encryptedAccountNumber, command.accountHolder());

        log.info("정산 항목 작성/수정 완료 - settleId={}", saved.getSettleId());

        // 활동 로그(항목 작성/수정) — text 도메인과 동일하게 실제로 바뀐 필드가 있을 때만 발행한다.
        // 계좌번호는 원문을 절대 로그에 남기지 않는다 — 이전/이후 값 모두 마스킹해서 비교·기록한다.
        List<ActivityFieldChange> changes = detectChanges(before, saved, command.accountNumber());
        if (!changes.isEmpty()) {
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.MODIFY,
                    saved.getBlockId(),
                    saved.getSettleId(),
                    saved.getTraderName(),
                    command.userId(),
                    changes
            ));
        }

        return toView(saved, command.accountNumber());
    }

    private SettlementType parseType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new ValidationException(SettlementErrorCode.TYPE_REQUIRED);
        }
        try {
            return SettlementType.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(SettlementErrorCode.TYPE_REQUIRED);
        }
    }

    // 금액(totalAmount/plannedAmount/plannedTaxAmount)은 음수를 막지 않는다 — 은행사 CSV/API 수집 양식에
    // 따라 OUTCOME 거래가 음수로 표기되는 경우가 있어, 여기서 부호를 강제하면 실제 데이터를 못 받는다.
    private void validateRequiredFields(UpdateSettlementItemCommand command) {
        if (command.roundNo() == null
                || command.totalAmount() == null
                || command.plannedAmount() == null
                || command.plannedTaxAmount() == null
                || command.plannedDate() == null
                || isBlank(command.traderName())) {
            throw new ValidationException(SettlementErrorCode.INVALID_CONTENT);
        }
        if (command.roundNo() <= 0) {
            throw new ValidationException(SettlementErrorCode.INVALID_CONTENT);
        }
    }

    private void validateOutcomeAccountInfo(UpdateSettlementItemCommand command) {
        if (isBlank(command.bankName()) || isBlank(command.accountNumber()) || isBlank(command.accountHolder())) {
            throw new ValidationException(SettlementErrorCode.OUTCOME_ACCOUNT_INFO_REQUIRED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** 세금계산서·입출금 내역이 연결되면(status != PENDING) 내용을 더 이상 고칠 수 없다. */
    private void assertModifiable(Settlement before) {
        if (before.getStatus() != SettlementStatus.PENDING) {
            log.warn("연결된 정산 블록 수정 시도 - settleId={}, status={}", before.getSettleId(), before.getStatus());
            throw new ConflictException(SettlementErrorCode.ALREADY_LINKED);
        }
    }

    /**
     * 같은 프로젝트·같은 타입의 다른 회차가 이미 총 예정 금액(totalAmount)을 정해뒀다면, 이번 요청도
     * 같은 값이어야 한다 — 프로젝트 시작 후에는 총 금액이 잘 안 바뀌고, paidAmountRatio 계산이
     * "회차마다 totalAmount가 같다"는 전제로 되어 있기 때문이다. 비교 대상이 여럿이면 이미 연결된
     * (status != PENDING) 회차의 값을 최우선으로 쓴다 — 그게 더 이상 안 바뀌는 진짜 확정값이다
     * (마이바티스 쿼리의 ORDER BY 로 우선순위를 매긴다). 아직 아무 회차도 값을 정한 적이 없으면(null)
     * 이번 요청이 그 프로젝트의 첫 기준값이 되므로 통과시킨다.
     */
    private void assertTotalAmountConsistent(Long settleId, SettlementType type, Long totalAmount) {
        Long establishedTotalAmount = settlementDetailMapper.findEstablishedTotalAmount(settleId, type.name());
        if (establishedTotalAmount != null && !establishedTotalAmount.equals(totalAmount)) {
            throw new ConflictException(SettlementErrorCode.TOTAL_AMOUNT_MISMATCH,
                    SettlementErrorCode.TOTAL_AMOUNT_MISMATCH.getMessage()
                            + " (기존 등록된 금액: " + establishedTotalAmount + "원)");
        }
    }

    private List<ActivityFieldChange> detectChanges(Settlement before, Settlement saved, String plainAccountNumber) {
        List<ActivityFieldChange> changes = new ArrayList<>();
        addIfChanged(changes, "roundNo", before.getRoundNo(), saved.getRoundNo());
        addIfChanged(changes, "type", before.getType(), saved.getType());
        addIfChanged(changes, "totalAmount", before.getTotalAmount(), saved.getTotalAmount());
        addIfChanged(changes, "plannedAmount", before.getPlannedAmount(), saved.getPlannedAmount());
        addIfChanged(changes, "plannedTaxAmount", before.getPlannedTaxAmount(), saved.getPlannedTaxAmount());
        addIfChanged(changes, "plannedDate", before.getPlannedDate(), saved.getPlannedDate());
        addIfChanged(changes, "traderName", before.getTraderName(), saved.getTraderName());
        addIfChanged(changes, "bankName", before.getBankName(), saved.getBankName());
        addIfChanged(changes, "accountHolder", before.getAccountHolder(), saved.getAccountHolder());

        // 계좌번호는 마스킹된 값으로만 비교·기록한다 — before는 저장된 암호문을 복호화 후 마스킹,
        // after는 이번 요청 평문을 마스킹한다. 원문은 어느 쪽도 changes에 담기지 않는다.
        String maskedBeforeAccountNumber = accountNumberCipher.decryptAndMask(before.getAccountNumber());
        String maskedAfterAccountNumber = accountNumberCipher.mask(plainAccountNumber);
        addIfChanged(changes, "accountNumber", maskedBeforeAccountNumber, maskedAfterAccountNumber);

        return changes;
    }

    private void addIfChanged(List<ActivityFieldChange> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            changes.add(new ActivityFieldChange(field,
                    before == null ? null : before.toString(),
                    after == null ? null : after.toString()));
        }
    }

    /**
     * paidAmountRatio 는 이 블록 하나의 값이 아니라 같은 프로젝트·같은 타입 정산 블록 전체의 진행률이라
     * (block → step → project 조인이 필요해) 저장 직후 {@link SettlementDetailMapper} 로 다시 조회해서 계산한다.
     * 목록 조회({@code SettlementBlockDetailAdapter})와 같은 쿼리·같은 계산식을 그대로 재사용한다.
     */
    private UpdateSettlementItemView toView(Settlement saved, String plainAccountNumber) {
        SettlementDetailRow row = settlementDetailMapper.findBySettleIds(List.of(saved.getSettleId())).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "settlement not found after update: " + saved.getSettleId()));

        return new UpdateSettlementItemView(
                saved.getSettleId(),
                saved.getRoundNo(),
                saved.getTotalAmount(),
                saved.getPlannedAmount(),
                saved.getPlannedTaxAmount(),
                saved.getPlannedDate(),
                saved.getTraderName(),
                saved.getBankName(),
                accountNumberCipher.mask(plainAccountNumber),
                saved.getAccountHolder(),
                saved.getActualAmount(),
                saved.getActualDate(),
                saved.getStatus(),
                SettlementProgress.ratio(row.actualAmountSum(), row.totalAmount()),
                saved.getCreatedAt()
        );
    }
}
