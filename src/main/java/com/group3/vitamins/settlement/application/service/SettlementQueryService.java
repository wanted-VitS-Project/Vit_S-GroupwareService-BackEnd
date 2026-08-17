package com.group3.vitamins.settlement.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.settlement.application.policy.SettlementEligibilityPolicy;
import com.group3.vitamins.settlement.application.port.PagePermissionPort;
import com.group3.vitamins.settlement.application.port.SettlementSiblingLookupPort;
import com.group3.vitamins.settlement.application.query.SettlementFilterQuery;
import com.group3.vitamins.settlement.application.query.SettlementProjectBlockListQuery;
import com.group3.vitamins.settlement.application.query.SettlementProjectListQuery;
import com.group3.vitamins.settlement.application.query.SettlementRecommendationQuery;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase;
import com.group3.vitamins.settlement.domain.exception.SettlementErrorCode;
import com.group3.vitamins.settlement.domain.model.Settlement;
import com.group3.vitamins.settlement.domain.model.SettlementType;
import com.group3.vitamins.settlement.infrastructure.security.AccountNumberCipher;
import com.group3.vitamins.settlement.infrastructure.status.SettlementProjectBlockRow;
import com.group3.vitamins.settlement.infrastructure.status.SettlementProjectRow;
import com.group3.vitamins.settlement.infrastructure.status.SettlementStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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
    private final SettlementSiblingLookupPort settlementSiblingLookupPort;
    private final SettlementStatusMapper settlementStatusMapper;
    private final AccountNumberCipher accountNumberCipher;
    private final PagePermissionPort pagePermissionPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

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
            SettlementSiblingLookupPort.SiblingRecommendation recommendation =
                    settlementSiblingLookupPort.findSiblingRecommendation(query.settleId(), type);
            // 삭제된 회차도 포함한 이력상 최댓값+1 — 회차 번호는 삭제돼도 재사용하지 않는다(2026-08-10).
            Long maxRoundNo = recommendation == null ? null : recommendation.maxRoundNo();
            recommendedRoundNo = (maxRoundNo == null ? 0 : maxRoundNo.intValue()) + 1;
            recommendedTotalAmount = recommendation == null ? null : recommendation.recommendedTotalAmount();
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

        return new SettlementFilterView(
                settlementStatusMapper.findDistinctClientNames(currentCompanyIdProvider.currentCompanyId()));
    }

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_PROJECT_SORTS =
            Set.of("NEXT_PLANNED_DATE_ASC", "TOTAL_AMOUNT_DESC");

    @Override
    public SettlementProjectListView getProjectSettlements(SettlementProjectListQuery query) {
        log.info("정산 현황 프로젝트 조회 요청 - userId={}", query.userId());

        if (!pagePermissionPort.hasAccess(FINANCE_PAGE_CODE, query.userId(), query.role())) {
            log.warn("재무 관리 페이지 접근 권한 없음 - userId={}", query.userId());
            throw new ForbiddenException(SettlementErrorCode.FINANCE_ACCESS_DENIED);
        }
        validateProjectListQuery(query);

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        List<SettlementProjectRow> rows = settlementStatusMapper.findProjectSettlements(
                query.startDate(), query.endDate(), query.client(), query.includeCompleted(),
                query.sort(), query.size(), query.page() * query.size(), companyId);
        long totalElements = settlementStatusMapper.countProjectSettlements(
                query.startDate(), query.endDate(), query.client(), query.includeCompleted(), companyId);
        int totalPages = (int) Math.ceil((double) totalElements / query.size());

        return new SettlementProjectListView(
                rows.stream().map(this::toProjectView).toList(),
                query.page(), query.size(), totalElements, totalPages
        );
    }

    // bidnotice 목록과 동일 컨벤션 — 잘못된 page/size/sort는 클램프 대신 400으로 던진다(silent clamp 아님).
    private void validateProjectListQuery(SettlementProjectListQuery query) {
        if (query.page() < 0 || query.size() <= 0 || query.size() > MAX_PAGE_SIZE
                || query.page() > Integer.MAX_VALUE / query.size()
                || (query.startDate() != null && query.endDate() != null
                && query.startDate().isAfter(query.endDate()))
                || (query.sort() != null && !ALLOWED_PROJECT_SORTS.contains(query.sort()))) {
            throw new ValidationException(SettlementErrorCode.PAGE_QUERY_INVALID);
        }
    }

    private SettlementProjectView toProjectView(SettlementProjectRow row) {
        long totalOutcome = row.totalOutcome() == null ? 0L : row.totalOutcome();
        long totalIncome = row.totalIncome() == null ? 0L : row.totalIncome();
        long completedRoundCount = row.completedRoundCount() == null ? 0L : row.completedRoundCount();
        long totalRoundCount = row.totalRoundCount() == null ? 0L : row.totalRoundCount();
        long pendingRoundCount = row.pendingRoundCount() == null ? 0L : row.pendingRoundCount();
        long taxInvoiceUnlinkedCount =
                row.taxInvoiceUnlinkedCount() == null ? 0L : row.taxInvoiceUnlinkedCount();
        long paymentOverdueDays = row.paymentOverdueDays() == null ? 0L : row.paymentOverdueDays();
        long taxInvoiceOverdueDays =
                row.taxInvoiceOverdueDays() == null ? 0L : row.taxInvoiceOverdueDays();

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
                (int) pendingRoundCount,
                (int) taxInvoiceUnlinkedCount,
                (int) paymentOverdueDays,
                (int) taxInvoiceOverdueDays,
                row.projectStatus(),
                row.endedOn()
        );
    }

    // settlementStatusSummary(문자열) 제거 (2026-08-14, 사용자 확정) — 원장별 미연결·지연을 전부 숫자로
    // 내려주게 되면서 이 문구가 담던 정보가 모두 계산 가능해졌다("정산완료"는
    // completedRoundCount == totalRoundCount, "미연결 N건"은 paymentUnlinkedCount). 서버가 표현(문구·색)을
    // 결정하지 않게 하려는 것이고, 애초에 문자열로 둔 이유(2026-08-09 "세분화 규칙이 없어서 단순화")도
    // 원장별 구분이 생기면서 사라졌다.

    @Override
    public SettlementProjectBlockListView getProjectSettlementBlocks(SettlementProjectBlockListQuery query) {
        log.info("정산 현황 블록 조회 요청 - projectId={}, userId={}", query.projectId(), query.userId());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        // 재무팀 정산현황 화면의 드릴다운이라 프로젝트 참여자 여부와 무관하게 FINANCE 페이지 권한으로
        // 판정한다 — 존재 확인이 권한 판정보다 먼저다(404가 403보다 앞선다, 팀 전체 컨벤션). 다른 회사의
        // projectId도 "존재하지 않음"과 동일하게 404로 처리한다(2026-08-11 추가, 크로스테넌트 방지).
        if (!settlementStatusMapper.existsActiveProject(query.projectId(), companyId)) {
            log.warn("존재하지 않는 프로젝트 - projectId={}", query.projectId());
            throw new NotFoundException(SettlementErrorCode.PROJECT_NOT_FOUND);
        }
        if (!pagePermissionPort.hasAccess(FINANCE_PAGE_CODE, query.userId(), query.role())) {
            log.warn("재무 관리 페이지 접근 권한 없음 - userId={}", query.userId());
            throw new ForbiddenException(SettlementErrorCode.FINANCE_ACCESS_DENIED);
        }

        List<SettlementProjectBlockRow> rows =
                settlementStatusMapper.findProjectSettlementBlocks(query.projectId(), companyId);

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
                // 마스킹 없이 원본 그대로 (2026-08-16, 담당자 확정) — 이 API는 FINANCE 페이지 권한자
                // (재무팀)만 도달하고, 재무팀은 실제 송금 처리를 위해 원본 계좌번호가 필요하다.
                row.accountNumber() == null ? null : accountNumberCipher.decrypt(row.accountNumber()),
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
