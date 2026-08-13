package com.group3.vitamins.finance.presentation.api.response;

import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.TaxInvoiceListView;
import com.group3.vitamins.finance.application.usecase.FinanceQueryUseCase.TaxInvoiceView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaxInvoiceListResponse(
        @Schema(description = "현재 페이지 번호 (0-base)", example = "0")
        int page,

        @Schema(description = "페이지당 개수", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "1")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "1")
        int totalPages,

        @Schema(description = "세금계산서 목록 (현재 페이지분만)")
        List<TaxInvoiceItem> taxInvoices
) {

    public static TaxInvoiceListResponse from(TaxInvoiceListView view) {
        return new TaxInvoiceListResponse(
                view.page(), view.size(), view.totalElements(), view.totalPages(),
                view.taxInvoices().stream().map(TaxInvoiceItem::from).toList()
        );
    }

    public record TaxInvoiceItem(
            @Schema(description = "세금계산서 ID", example = "1")
            Long taxId,
            @Schema(description = "발행일", example = "2026-07-20")
            LocalDate issuedNo,
            @Schema(description = "승인번호", example = "20260720-12345678")
            String approvalNo,
            @Schema(description = "구분 (INCOME/OUTCOME)", example = "INCOME")
            String type,
            @Schema(description = "공급받는자 상호명", example = "환경부")
            String buyerName,
            @Schema(description = "공급받는자 사업자번호", example = "1234567890")
            String buyerBizNo,
            @Schema(description = "공급자 사업자번호", example = "9876543210", nullable = true)
            String supplierBizNo,
            @Schema(description = "종사업장번호", example = "0001", nullable = true)
            String subBizNo,
            @Schema(description = "대표자명", example = "홍길동", nullable = true)
            String ceoName,
            @Schema(description = "품목명", example = "환경개선 컨설팅 용역", nullable = true)
            String itemName,
            @Schema(description = "공급가액", example = "40909090")
            BigDecimal supplyAmount,
            @Schema(description = "세액", example = "4090910")
            BigDecimal taxAmount,
            @Schema(description = "합계", example = "45000000")
            BigDecimal totalAmount,
            @Schema(description = "비고/메모", example = "선급금", nullable = true)
            String memo,
            // DB enum에는 HOMETAX_API도 있지만 그 값을 만드는 코드 경로가 없다 — 구현되지 않은 출처를
            // 계약에 노출하면 프론트가 없는 분기를 짜게 되므로 CSV만 적는다(홈택스 연동이 생기면 추가).
            @Schema(description = "수집 출처. 현재는 CSV 업로드로만 유입된다", example = "CSV",
                    allowableValues = {"CSV"})
            String sourceType,
            @Schema(description = "연결된 프로젝트 ID. 미연결이거나 프로젝트 자체가 삭제됐으면 null", example = "1", nullable = true)
            Long projectId,
            @Schema(description = "연결 프로젝트명. 미연결이거나 프로젝트 자체가 삭제됐으면 null", example = "한강 생태교육 환경개선사업", nullable = true)
            String projectName,
            @Schema(description = "연결된 정산 블록 아이디. 미연결이면 null — 블록이 삭제돼도 값은 유지됨(linkStatus 참고)",
                    example = "10", nullable = true)
            Long settleId,
            @Schema(description = "연결된 정산 블록명. 미연결이면 null — 블록이 삭제돼도 값은 유지됨(linkStatus 참고)",
                    example = "1차 정산(선급 60%)", nullable = true)
            String roundName,
            @Schema(description = "매칭 처리자 사번. 미연결이면 null", example = "vitas-EMP004", nullable = true)
            String linkedBy,
            @Schema(description = "매칭 처리자 이름. 미연결이면 null", example = "김재무", nullable = true)
            String linkedByName,
            @Schema(description = "매칭 일시. 미연결이면 null", example = "2026-06-30T14:00:00", nullable = true)
            LocalDateTime linkedAt,
            @Schema(description = "연결 제외 여부", example = "false")
            boolean isExcluded,
            @Schema(description = "정산 블록 연결 상태 — UNLINKED(미연결)/LINKED(연결됨)/"
                    + "LINK_BLOCK_DELETED(연결됐던 정산 블록이 삭제됨). 원 명세엔 없던 필드, cash_flow와 동일 컨벤션",
                    example = "UNLINKED")
            String linkStatus
    ) {

        public static TaxInvoiceItem from(TaxInvoiceView view) {
            return new TaxInvoiceItem(
                    view.taxId(), view.issuedNo(), view.approvalNo(), view.type(), view.buyerName(),
                    view.buyerBizNo(), view.supplierBizNo(), view.subBizNo(), view.ceoName(), view.itemName(),
                    view.supplyAmount(), view.taxAmount(), view.totalAmount(), view.memo(), view.sourceType(),
                    view.projectId(), view.projectName(), view.settleId(), view.roundName(),
                    view.linkedBy(), view.linkedByName(), view.linkedAt(), view.isExcluded(), view.linkStatus()
            );
        }
    }
}
