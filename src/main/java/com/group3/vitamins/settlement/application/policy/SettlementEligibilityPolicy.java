package com.group3.vitamins.settlement.application.policy;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.settlement.domain.exception.SettlementErrorCode;
import com.group3.vitamins.settlement.domain.model.Settlement;
import com.group3.vitamins.settlement.domain.model.SettlementType;
import com.group3.vitamins.settlement.domain.repository.SettlementRepository;
import com.group3.vitamins.text.application.port.BlockCatalogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link BlockCatalogPort} 는 text 도메인이 정의한 공용 포트를 재사용한다 —
 * 이 프로젝트는 도메인마다 새로 안 만들고 구현체 하나를 공유하는 컨벤션이다 (checklist·image 동일).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEligibilityPolicy {

    private static final String BLOCK_TYPE = "SETTLEMENT";

    private final BlockCatalogPort blockCatalogPort;
    private final SettlementRepository settlementRepository;

    public Settlement getActiveSettlementOrThrow(Long settleId) {
        return settlementRepository.findActiveBySettleId(settleId)
                .orElseThrow(() -> {
                    log.warn("정산 블록 존재하지 않음 - settleId={}", settleId);
                    return new NotFoundException(SettlementErrorCode.BLOCK_NOT_FOUND);
                });
    }

    public void assertEditPermission(Long settleId, String userId, String role) {
        if (!blockCatalogPort.hasEditPermission(BLOCK_TYPE, settleId, userId, role)) {
            log.warn("편집 권한 없음 - blockType={}, settleId={}, userId={}", BLOCK_TYPE, settleId, userId);
            throw new ForbiddenException(SettlementErrorCode.FORBIDDEN);
        }
    }

    /**
     * OUTCOME → INCOME 전환/조회는 막는다 — OUTCOME 전용 필드(계좌정보)를 버려야 하는 손실성 변경이라
     * PATCH(항목 작성/수정)와 GET(추천 조회) 양쪽에서 같은 규칙을 쓴다. INCOME → OUTCOME(계좌정보를 새로
     * 받기만 하면 됨)과 최초 작성(storedType == null, 아직 빈 블록)은 허용한다.
     */
    public void assertNoTypeDowngrade(SettlementType storedType, SettlementType requestedType) {
        if (storedType == SettlementType.OUTCOME && requestedType == SettlementType.INCOME) {
            throw new ConflictException(SettlementErrorCode.TYPE_DOWNGRADE_NOT_ALLOWED);
        }
    }
}
