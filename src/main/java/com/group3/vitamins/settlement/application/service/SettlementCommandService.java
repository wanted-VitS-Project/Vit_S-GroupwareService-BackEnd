package com.group3.vitamins.settlement.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.settlement.application.command.UpdateSettlementItemCommand;
import com.group3.vitamins.settlement.application.policy.SettlementEligibilityPolicy;
import com.group3.vitamins.settlement.application.port.SettlementSiblingLookupPort;
import com.group3.vitamins.settlement.application.usecase.SettlementCommandUseCase;
import com.group3.vitamins.settlement.domain.exception.SettlementErrorCode;
import com.group3.vitamins.settlement.domain.model.Settlement;
import com.group3.vitamins.settlement.domain.model.SettlementProgress;
import com.group3.vitamins.settlement.domain.model.SettlementStatus;
import com.group3.vitamins.settlement.domain.model.SettlementType;
import com.group3.vitamins.settlement.domain.repository.SettlementRepository;
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
    private final SettlementSiblingLookupPort settlementSiblingLookupPort;

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

        // 편의상 빠른 실패용 검사다(잠금 이전에 읽은 before 기준) — 진짜 방어는 아래 잠금 후 재조회다.
        assertModifiable(before);

        // SETL-008 검증(조회) 전에 같은 프로젝트의 정산 블록 전체를 잠근다 — 두 개의 빈 블록이 동시에
        // PATCH되면서 서로 "기준값 없음"으로 읽고 다른 totalAmount를 저장하는 레이스를 막는다.
        settlementSiblingLookupPort.lockSiblingSettlementBlocksForUpdate(command.settleId());

        // 잠금 직후 이 행의 현재(최신 커밋) 상태 전체를 다시 읽는다. 이 판정 이후로는 이 트랜잭션이 끝날
        // 때까지 아무도 이 행을 못 바꾸므로(FOR UPDATE로 계속 잠겨 있음):
        // 1) 삭제·연결 여부를 여기서 판정하면, 아래 조건부 UPDATE가 0건이 되는 원인은 버전 불일치뿐임이
        //    보장된다 — "갱신 실패 후 일반 조회로 원인 재분류"는 REPEATABLE READ 스냅샷 때문에 부정확할 수
        //    있다는 지적(CodeRabbit, 2026-08-12)을 "쓰기 전에 잠금 하에 미리 확정"으로 근본 해결한 것이다.
        // 2) 타입 다운그레이드 판정·활동 로그 이전값 비교도 여기서 읽은 값을 써야 한다 — before(잠금 이전
        //    값)로 하면 그 사이 남이 타입/내용을 바꿔도 옛 값 기준으로 잘못 판정·기록된다(CodeRabbit,
        //    2026-08-12 2차 지적).
        SettlementSiblingLookupPort.SettlementCurrentState currentState =
                settlementSiblingLookupPort.findCurrentStateForUpdate(command.settleId());
        if (currentState == null || currentState.deletedAt() != null) {
            throw new NotFoundException(SettlementErrorCode.BLOCK_NOT_FOUND);
        }
        if (!SettlementStatus.PENDING.name().equals(currentState.status())) {
            log.warn("연결된 정산 블록 수정 시도 - settleId={}, status={}", command.settleId(), currentState.status());
            throw new ConflictException(SettlementErrorCode.ALREADY_LINKED);
        }
        // currentState.type()은 한 번도 작성된 적 없는 빈 블록이면 null이다(2026-08-12, NPE로 발견) —
        // assertNoTypeDowngrade는 storedType==null을 "다운그레이드 대상 없음"으로 이미 처리하므로 null을
        // 그대로 넘긴다. SettlementType.valueOf(null)은 IllegalArgumentException이 아니라 NPE라
        // 기존 어디서도 못 잡고 그대로 500이 됐다.
        SettlementType storedType = currentState.type() == null ? null : SettlementType.valueOf(currentState.type());
        eligibilityPolicy.assertNoTypeDowngrade(storedType, type);

        assertTotalAmountConsistent(command.settleId(), type, command.totalAmount());

        // INCOME 블록에는 계좌정보가 존재할 수 없다(2026-08-14) — 화면은 입력을 막지만 API를 직접 호출하면
        // 그대로 저장돼 "계좌정보는 OUTCOME만 값 있음"이라는 명세 약속이 깨졌다. 새 에러코드로 거부하는
        // 대신 서버에서 버린다(정상 클라이언트는 애초에 보내지 않는다). 아래 활동 로그·응답 조립도 반드시
        // 이 plainAccountNumber를 써야 한다 — 그 둘은 DB에 저장된 값이 아니라 요청값을 직접 들고 가므로,
        // command.accountNumber()를 그대로 넘기면 저장은 안 됐는데 가짜 변경 로그가 남고 응답에만
        // 마스킹된 계좌번호가 실려 나간다.
        boolean outcome = type == SettlementType.OUTCOME;
        String bankName = outcome ? command.bankName() : null;
        String accountHolder = outcome ? command.accountHolder() : null;
        String plainAccountNumber = outcome ? command.accountNumber() : null;

        String encryptedAccountNumber = plainAccountNumber == null
                ? null
                : accountNumberCipher.encrypt(plainAccountNumber);

        // overwrite면 위에서 잠금 하에 다시 읽은 "지금" 버전을 기대값으로 써서 반드시 통과시킨다. 아니면
        // 클라이언트가 보낸 버전을 그대로 검사한다(WHERE version = ?, CONCURRENCY.md §1-5).
        int expectedVersion = command.overwrite() ? currentState.version() : command.version();

        Settlement saved = settlementRepository.updateItem(
                command.settleId(), type, command.roundNo(), command.totalAmount(),
                command.plannedAmount(), command.plannedTaxAmount(), command.plannedDate(),
                command.taxInvoiceDueDate(), command.traderName(), bankName, encryptedAccountNumber, accountHolder,
                expectedVersion);

        log.info("정산 항목 작성/수정 완료 - settleId={}", saved.getSettleId());

        // 활동 로그(항목 작성/수정) — text 도메인과 동일하게 실제로 바뀐 필드가 있을 때만 발행한다.
        // 계좌번호는 원문을 절대 로그에 남기지 않는다 — 이전/이후 값 모두 마스킹해서 비교·기록한다.
        List<ActivityFieldChange> changes = detectChanges(currentState, saved, plainAccountNumber);
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

        return toView(saved, plainAccountNumber);
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
            throw new ValidationException(SettlementErrorCode.ROUND_NO_INVALID);
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
        Long establishedTotalAmount = settlementSiblingLookupPort.findEstablishedTotalAmount(settleId, type);
        if (establishedTotalAmount != null && !establishedTotalAmount.equals(totalAmount)) {
            throw new ConflictException(SettlementErrorCode.TOTAL_AMOUNT_MISMATCH,
                    SettlementErrorCode.TOTAL_AMOUNT_MISMATCH.getMessage()
                            + " (기존 등록된 금액: " + establishedTotalAmount + "원)");
        }
    }

    private List<ActivityFieldChange> detectChanges(
            SettlementSiblingLookupPort.SettlementCurrentState before, Settlement saved, String plainAccountNumber
    ) {
        List<ActivityFieldChange> changes = new ArrayList<>();
        addIfChanged(changes, "roundNo", before.roundNo(), saved.getRoundNo());
        addIfChanged(changes, "type", before.type(), saved.getType().name());
        addIfChanged(changes, "totalAmount", before.totalAmount(), saved.getTotalAmount());
        addIfChanged(changes, "plannedAmount", before.plannedAmount(), saved.getPlannedAmount());
        addIfChanged(changes, "plannedTaxAmount", before.plannedTaxAmount(), saved.getPlannedTaxAmount());
        addIfChanged(changes, "plannedDate", before.plannedDate(), saved.getPlannedDate());
        addIfChanged(changes, "taxInvoiceDueDate", before.taxInvoiceDueDate(), saved.getTaxInvoiceDueDate());
        addIfChanged(changes, "traderName", before.traderName(), saved.getTraderName());
        addIfChanged(changes, "bankName", before.bankName(), saved.getBankName());
        addIfChanged(changes, "accountHolder", before.accountHolder(), saved.getAccountHolder());

        // 변경 여부는 원문으로 판단한다 — 마스킹은 앞·뒤 3자리만 남겨서, 서로 다른 계좌번호가 같은
        // 마스킹값이 될 수 있다(예: "100111111444"/"100999999444" 둘 다 "100******444"). 마스킹값으로
        // 비교하면 그런 변경이 조용히 로그에서 빠진다. 로그에 기록하는 값은 여전히 마스킹된 값만이다 —
        // 비교에만 원문을 쓰고, changes에는 원문을 절대 담지 않는다.
        String beforePlainAccountNumber = before.accountNumber() == null
                ? null
                : accountNumberCipher.decrypt(before.accountNumber());
        if (!Objects.equals(beforePlainAccountNumber, plainAccountNumber)) {
            changes.add(new ActivityFieldChange("accountNumber",
                    accountNumberCipher.decryptAndMask(before.accountNumber()),
                    accountNumberCipher.mask(plainAccountNumber)));
        }

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
     * (block → step → project 조인이 필요해) 저장 직후 {@link SettlementSiblingLookupPort} 로 다시 조회해서
     * 계산한다. 목록 조회({@code SettlementBlockDetailAdapter})와 같은 계산식(`SettlementProgress.ratio`)을
     * 그대로 재사용한다.
     */
    private UpdateSettlementItemView toView(Settlement saved, String plainAccountNumber) {
        Long actualAmountSum = settlementSiblingLookupPort.findActualAmountSum(saved.getSettleId(), saved.getType());

        return new UpdateSettlementItemView(
                saved.getSettleId(),
                saved.getRoundNo(),
                saved.getTotalAmount(),
                saved.getPlannedAmount(),
                saved.getPlannedTaxAmount(),
                saved.getPlannedDate(),
                saved.getTaxInvoiceDueDate(),
                saved.getTraderName(),
                saved.getBankName(),
                accountNumberCipher.mask(plainAccountNumber),
                saved.getAccountHolder(),
                saved.getActualAmount(),
                saved.getActualDate(),
                saved.getStatus(),
                SettlementProgress.ratio(actualAmountSum, saved.getTotalAmount()),
                saved.getCreatedAt(),
                saved.getVersion()
        );
    }
}
