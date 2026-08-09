package com.group3.vitamins.settlement.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.settlement.application.policy.SettlementEligibilityPolicy;
import com.group3.vitamins.settlement.application.port.PagePermissionPort;
import com.group3.vitamins.settlement.application.query.SettlementFilterQuery;
import com.group3.vitamins.settlement.application.query.SettlementProjectBlockListQuery;
import com.group3.vitamins.settlement.application.query.SettlementProjectListQuery;
import com.group3.vitamins.settlement.application.query.SettlementRecommendationQuery;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase;
import com.group3.vitamins.settlement.domain.exception.SettlementErrorCode;
import com.group3.vitamins.settlement.domain.model.Settlement;
import com.group3.vitamins.settlement.domain.model.SettlementType;
import com.group3.vitamins.settlement.infrastructure.blockdetail.SettlementDetailMapper;
import com.group3.vitamins.settlement.infrastructure.blockdetail.SettlementRecommendationRow;
import com.group3.vitamins.settlement.infrastructure.security.AccountNumberCipher;
import com.group3.vitamins.settlement.infrastructure.status.SettlementProjectBlockRow;
import com.group3.vitamins.settlement.infrastructure.status.SettlementProjectRow;
import com.group3.vitamins.settlement.infrastructure.status.SettlementStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 정산 항목 수정 화면의 "타입 변경 탭" 클릭 시 호출되는 조회 전용 API.
 *
 * <p>이 블록이 **아직 빈 채(최초 작성 전, roundNo == null)**면 회차/총 금액 추천값을 내려주고,
 * 계좌번호는 어차피 저장된 적이 없어 항상 null이다. 반대로 이 블록에 **이미 내용이 채워져 있으면**
 * (재수정) 추천값은 의미가 없어 null로 내려주고, 대신 `OUTCOME`이면 저장된 원본(마스킹 없는) 계좌번호를
 * 내려준다 — 목록 조회·PATCH 응답은 항상 마스킹된 계좌번호만 다루는 것과 달리, 이 엔드포인트만
 * 예외적으로 원문을 돌려준다(수정 폼에 기존 계좌번호를 그대로 채워주기 위함).
 *
 * <p>이미 채워진 블록에 저장된 타입과 다른 타입을 요청하면(OUTCOME으로 저장돼 있는데 INCOME으로 조회)
 * {@link SettlementEligibilityPolicy#assertNoTypeDowngrade}가 PATCH와 동일하게 막는다 — 어차피
 * 저장 시점에 막힐 전환을 조회 단계에서 미리 알려준다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SettlementQueryService implements SettlementQueryUseCase {

    private static final String FINANCE_PAGE_CODE = "FINANCE";

    private final SettlementEligibilityPolicy eligibilityPolicy;
    private final SettlementDetailMapper settlementDetailMapper;
    private final SettlementStatusMapper settlementStatusMapper;
    private final AccountNumberCipher accountNumberCipher;
    private final PagePermissionPort pagePermissionPort;

    @Override
    public SettlementRecommendationView getRecommendation(SettlementRecommendationQuery query) {
        log.info("정산 항목 수정 시 조회 요청 - settleId={}, type={}, userId={}",
                query.settleId(), query.type(), query.userId());

        SettlementType type = parseType(query.type());

        Settlement settlement = eligibilityPolicy.getActiveSettlementOrThrow(query.settleId());
        eligibilityPolicy.assertEditPermission(query.settleId(), query.userId(), query.role());
        eligibilityPolicy.assertNoTypeDowngrade(settlement.getType(), type);

        // roundNo는 항목 작성/수정 API의 공통 필수 필드(SETL-003)라, null이면 아직 한 번도 작성된 적
        // 없는 빈 블록이라는 뜻이다. 그 경우에만 추천값을 계산한다 — 이미 내용이 있으면 추천이 무의미하다.
        boolean isEmpty = settlement.getRoundNo() == null;

        Integer recommendedRoundNo = null;
        Long recommendedTotalAmount = null;
        if (isEmpty) {
            SettlementRecommendationRow row =
                    settlementDetailMapper.findRecommendation(query.settleId(), type.name());
            long siblingCount = row == null || row.blockCount() == null ? 0 : row.blockCount();
            recommendedRoundNo = (int) siblingCount + 1;
            recommendedTotalAmount = row == null ? null : row.recommendedTotalAmount();
        }

        // 이 블록에 이미 저장된 타입이 아니라, 지금 사용자가 고른(쿼리파라미터) 타입 기준으로 판단한다 —
        // "타입 변경 탭"에서 호출되므로 화면에 표시 중인 타입이 저장된 값과 다를 수 있다. 빈 블록이면
        // 애초에 accountNumber가 저장된 적이 없어 이 조건이 자연히 null로 떨어진다.
        String originalAccountNumber = type == SettlementType.OUTCOME && settlement.getAccountNumber() != null
                ? accountNumberCipher.decrypt(settlement.getAccountNumber())
                : null;

        return new SettlementRecommendationView(
                settlement.getSettleId(),
                recommendedRoundNo,
                recommendedTotalAmount,
                originalAccountNumber
        );
    }

    @Override
    public SettlementFilterView getFilters(SettlementFilterQuery query) {
        log.info("정산현황 필터 옵션 조회 요청 - userId={}", query.userId());

        if (!pagePermissionPort.hasAccess(FINANCE_PAGE_CODE, query.userId(), query.role())) {
            log.warn("재무 관리 페이지 접근 권한 없음 - userId={}", query.userId());
            throw new ForbiddenException(SettlementErrorCode.FINANCE_ACCESS_DENIED);
        }

        return new SettlementFilterView(settlementStatusMapper.findDistinctClientNames());
    }

    @Override
    public SettlementProjectListView getProjectSettlements(SettlementProjectListQuery query) {
        log.info("정산 현황 프로젝트 조회 요청 - userId={}", query.userId());

        if (!pagePermissionPort.hasAccess(FINANCE_PAGE_CODE, query.userId(), query.role())) {
            log.warn("재무 관리 페이지 접근 권한 없음 - userId={}", query.userId());
            throw new ForbiddenException(SettlementErrorCode.FINANCE_ACCESS_DENIED);
        }

        List<SettlementProjectRow> rows = settlementStatusMapper.findProjectSettlements(
                query.startDate(), query.endDate(), query.client(), query.includeCompleted());

        return new SettlementProjectListView(rows.stream().map(this::toProjectView).toList());
    }

    private SettlementProjectView toProjectView(SettlementProjectRow row) {
        long totalOutcome = row.totalOutcome() == null ? 0L : row.totalOutcome();
        long totalIncome = row.totalIncome() == null ? 0L : row.totalIncome();
        long completedRoundCount = row.completedRoundCount() == null ? 0L : row.completedRoundCount();
        long totalRoundCount = row.totalRoundCount() == null ? 0L : row.totalRoundCount();
        long pendingRoundCount = row.pendingRoundCount() == null ? 0L : row.pendingRoundCount();

        return new SettlementProjectView(
                row.projectId(),
                row.projectName(),
                row.clientName(),
                row.projectManager(),
                row.totalPlannedAmount(),
                totalOutcome,
                totalIncome,
                totalIncome - totalOutcome,
                (int) completedRoundCount,
                (int) totalRoundCount,
                row.nextPlannedDate(),
                settlementStatusSummary(totalRoundCount, completedRoundCount, pendingRoundCount),
                row.projectStatus(),
                row.endedOn()
        );
    }

    // 회차 하나하나의 예정일까지 따지는 세분화(계산서 미발행·입금 대기 N일 등)는 아직 안 한다 — 지금은
    // "전부 완료됐는지"와 "미연결(PENDING) 회차가 몇 건인지"만 구분한다. 추후 규칙이 늘어날 수 있다.
    private String settlementStatusSummary(long totalRoundCount, long completedRoundCount, long pendingRoundCount) {
        if (totalRoundCount > 0 && completedRoundCount == totalRoundCount) {
            return "정산완료";
        }
        return "미연결 " + pendingRoundCount + "건";
    }

    @Override
    public SettlementProjectBlockListView getProjectSettlementBlocks(SettlementProjectBlockListQuery query) {
        log.info("정산 현황 블록 조회 요청 - projectId={}, userId={}", query.projectId(), query.userId());

        // 재무팀 정산현황 화면의 드릴다운이라 프로젝트 참여자 여부와 무관하게 FINANCE 페이지 권한으로
        // 판정한다 — 존재 확인이 권한 판정보다 먼저다(404가 403보다 앞선다, 팀 전체 컨벤션).
        if (!settlementStatusMapper.existsActiveProject(query.projectId())) {
            log.warn("존재하지 않는 프로젝트 - projectId={}", query.projectId());
            throw new NotFoundException(SettlementErrorCode.PROJECT_NOT_FOUND);
        }
        if (!pagePermissionPort.hasAccess(FINANCE_PAGE_CODE, query.userId(), query.role())) {
            log.warn("재무 관리 페이지 접근 권한 없음 - userId={}", query.userId());
            throw new ForbiddenException(SettlementErrorCode.FINANCE_ACCESS_DENIED);
        }

        List<SettlementProjectBlockRow> rows =
                settlementStatusMapper.findProjectSettlementBlocks(query.projectId());

        return new SettlementProjectBlockListView(rows.stream().map(this::toBlockView).toList());
    }

    private SettlementProjectBlockView toBlockView(SettlementProjectBlockRow row) {
        return new SettlementProjectBlockView(
                row.settleId(),
                row.roundNo(),
                row.roundName(),
                row.plannedDate(),
                row.plannedAmount(),
                row.plannedTaxAmount(),
                row.taxInvoiceDate(),
                row.taxInvoiceAmount(),
                row.paidType(),
                row.bankName(),
                accountNumberCipher.decryptAndMask(row.accountNumber()),
                row.accountHolder(),
                row.paidDate(),
                row.paidAmount(),
                row.status(),
                row.taxLinkedBy(),
                row.taxLinkedByName(),
                row.taxLinkedAt(),
                row.cashFlowLinkedBy(),
                row.cashFlowLinkedByName(),
                row.cashFlowLinkedAt()
        );
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
}
