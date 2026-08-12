package com.group3.vitamins.settlement.presentation;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.settlement.application.command.UpdateSettlementItemCommand;
import com.group3.vitamins.settlement.application.query.SettlementFilterQuery;
import com.group3.vitamins.settlement.application.query.SettlementProjectBlockListQuery;
import com.group3.vitamins.settlement.application.query.SettlementProjectListQuery;
import com.group3.vitamins.settlement.application.query.SettlementRecommendationQuery;
import com.group3.vitamins.settlement.application.usecase.SettlementCommandUseCase;
import com.group3.vitamins.settlement.application.usecase.SettlementCommandUseCase.UpdateSettlementItemView;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementFilterView;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementProjectBlockListView;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementProjectListView;
import com.group3.vitamins.settlement.application.usecase.SettlementQueryUseCase.SettlementRecommendationView;
import com.group3.vitamins.settlement.presentation.api.request.SettlementItemUpsertRequest;
import com.group3.vitamins.settlement.presentation.api.response.SettlementFilterResponse;
import com.group3.vitamins.settlement.presentation.api.response.SettlementItemResponse;
import com.group3.vitamins.settlement.presentation.api.response.SettlementProjectBlockListResponse;
import com.group3.vitamins.settlement.presentation.api.response.SettlementProjectListResponse;
import com.group3.vitamins.settlement.presentation.api.response.SettlementRecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 정산 블록 API.
 *
 * <p>생성·삭제는 이 컨트롤러에 없다 — 블록 생성/삭제는 Block 도메인이 전부 처리한다.
 * 여기는 정산 항목(내용) 작성/수정만 담당한다.
 */
@Tag(name = "Settlement", description = "정산 블록 API")
@RestController
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementCommandUseCase settlementCommandUseCase;
    private final SettlementQueryUseCase settlementQueryUseCase;

    @Operation(summary = "정산 항목 수정 시 조회",
            description = "정산 항목 수정 화면의 타입 변경 탭 클릭 시 호출한다. 그 타입(INCOME/OUTCOME) 기준으로 "
                    + "추천 회차 번호·추천 총 금액과, OUTCOME인 경우 마스킹 없는 원본 계좌번호를 내려준다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정산 항목 수정 시 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "정산 블록의 타입 지정은 필수입니다. (SETL-005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "편집 권한이 없습니다. (SETL-001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 블록입니다. (SETL-002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "출금(OUTCOME)에서 입금(INCOME)으로는 타입을 변경할 수 없습니다. (SETL-006)")
    })
    @GetMapping("/api/v1/blocks/settlements/{settleId}/items")
    public ResponseEntity<ApiResponse<SettlementRecommendationResponse>> getRecommendation(
            @Parameter(description = "항목을 수정할 정산 블록 ID", example = "1")
            @PathVariable Long settleId,
            @Parameter(description = "지금 화면에서 선택 중인 타입. 이 타입 기준으로 추천값을 계산한다", example = "INCOME",
                    required = true)
            @RequestParam(required = false) String type,
            Authentication authentication
    ) {
        SettlementRecommendationView view = settlementQueryUseCase.getRecommendation(
                new SettlementRecommendationQuery(settleId, type, authentication.getName(),
                        RequesterRole.from(authentication)));

        // 이미 채워진 OUTCOME 블록이면 마스킹 없는 원본 계좌번호가 실려 나간다 — 브라우저/프록시가
        // 이 응답을 캐시해 사본을 남기지 않도록 명시적으로 캐시를 막는다.
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(ApiResponse.success("정산 항목 수정 시 조회 성공",
                        SettlementRecommendationResponse.from(view)));
    }

    @Operation(summary = "정산 항목 작성/수정", description = "정산 블록 내부의 정산 항목을 작성하거나 수정한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정산 항목 작성/수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "정산 블록의 타입 지정은 필수입니다. (SETL-005) / 내용을 입력해 주세요. (SETL-003) "
                            + "/ 출금 타입은 계좌정보가 필수입니다. (SETL-004) / 회차 번호는 1 이상이어야 합니다. (SETL-011) "
                            + "/ 버전 정보가 없습니다. (SETTLEMENT_VERSION_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "편집 권한이 없습니다. (SETL-001)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 블록입니다. (SETL-002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "출금(OUTCOME)에서 입금(INCOME)으로는 타입을 변경할 수 없습니다. (SETL-006) "
                            + "/ 세금계산서 또는 입출금 내역이 연결되어 있어 수정할 수 없습니다. (SETL-007) "
                            + "/ 같은 프로젝트의 다른 정산 블록과 총 예정 금액이 일치하지 않습니다. (SETL-008) "
                            + "/ 다른 사용자가 먼저 수정했습니다. (SETTLEMENT_VERSION_CONFLICT)")
    })
    @PatchMapping("/api/v1/blocks/settlements/{settleId}/items")
    public ResponseEntity<ApiResponse<SettlementItemResponse>> upsertItem(
            @Parameter(description = "정산 내용을 작성할 정산 블록 ID", example = "1")
            @PathVariable Long settleId,
            @Parameter(description = "우리 회사 입장에서 입금(INCOME)인지 출금(OUTCOME)인지 여부", example = "INCOME",
                    required = true)
            @RequestParam(required = false) String type,
            @Valid @RequestBody SettlementItemUpsertRequest request,
            Authentication authentication
    ) {
        UpdateSettlementItemView view = settlementCommandUseCase.upsertItem(new UpdateSettlementItemCommand(
                authentication.getName(),
                settleId,
                type,
                request.roundNo(),
                request.totalAmount(),
                request.plannedAmount(),
                request.plannedTaxAmount(),
                request.plannedDate(),
                request.traderName(),
                request.bankName(),
                request.accountNumber(),
                request.accountHolder(),
                request.version(),
                Boolean.TRUE.equals(request.overwrite()),
                RequesterRole.from(authentication)
        ));

        return ResponseEntity.ok(ApiResponse.success("정산 항목 작성/수정 성공", SettlementItemResponse.from(view)));
    }

    @Operation(summary = "정산현황 필터 옵션 조회",
            description = "재무팀 정산현황 화면의 발주처 필터 옵션을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정산현황 필터 옵션 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다. (SETL-009)")
    })
    @GetMapping("/api/v1/projects/settlements/filters")
    public ResponseEntity<ApiResponse<SettlementFilterResponse>> getSettlementFilters(
            Authentication authentication
    ) {
        SettlementFilterView view = settlementQueryUseCase.getFilters(
                new SettlementFilterQuery(authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("정산현황 필터 옵션 조회 성공",
                SettlementFilterResponse.from(view)));
    }

    @Operation(summary = "정산 현황 프로젝트 조회",
            description = "재무팀 쪽에서 보일 전체 프로젝트에 대한 프로젝트 단위 정산 현황을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정산 현황 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "페이지 조회 조건이 올바르지 않습니다. (SETL-012)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다. (SETL-009)")
    })
    @GetMapping("/api/v1/projects/settlements")
    public ResponseEntity<ApiResponse<SettlementProjectListResponse>> getProjectSettlements(
            @Parameter(description = "다음 정산 예정일 기준 조회 시작일", example = "2026-09-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "다음 정산 예정일 기준 조회 종료일", example = "2026-09-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "발주처", example = "환경부")
            @RequestParam(required = false) String client,
            @Parameter(description = "종결(완료) 프로젝트 포함 여부. 생략하면 false(제외)", example = "false")
            @RequestParam(required = false) Boolean includeCompleted,
            @Parameter(description = "0-base 페이지 번호. 생략하면 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 개수(최대 100). 생략하면 20", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준. 생략하면 NEXT_PLANNED_DATE_ASC",
                    example = "NEXT_PLANNED_DATE_ASC",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(
                            allowableValues = {"NEXT_PLANNED_DATE_ASC", "TOTAL_AMOUNT_DESC"}))
            @RequestParam(defaultValue = "NEXT_PLANNED_DATE_ASC") String sort,
            Authentication authentication
    ) {
        SettlementProjectListView view = settlementQueryUseCase.getProjectSettlements(
                new SettlementProjectListQuery(startDate, endDate, client, includeCompleted, page, size, sort,
                        authentication.getName(), RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("정산 현황 조회 성공",
                SettlementProjectListResponse.from(view)));
    }

    @Operation(summary = "정산 현황 블록 조회",
            description = "재무팀 정산현황 화면에서 하나의 프로젝트에 속한 정산 블록을 회차별로 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정산 현황 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다. (SETL-009)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 프로젝트입니다. (SETL-010)")
    })
    @GetMapping("/api/v1/projects/{projectId}/settlements")
    public ResponseEntity<ApiResponse<SettlementProjectBlockListResponse>> getProjectSettlementBlocks(
            @Parameter(description = "정산 블록 정보를 조회할 프로젝트 ID", example = "1")
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        SettlementProjectBlockListView view = settlementQueryUseCase.getProjectSettlementBlocks(
                new SettlementProjectBlockListQuery(projectId, authentication.getName(),
                        RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("정산 현황 조회 성공",
                SettlementProjectBlockListResponse.from(view)));
    }
}
