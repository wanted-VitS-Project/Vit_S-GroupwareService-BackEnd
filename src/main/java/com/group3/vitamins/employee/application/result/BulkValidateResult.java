package com.group3.vitamins.employee.application.result;

import java.util.List;

/** 엑셀 일괄 등록 <b>검증(§7)</b> 결과. 오류가 있어도 성공(200)이며, 행별 오류를 담아 화면 스텝퍼 ②가 보여준다. */
public record BulkValidateResult(
        int totalRows,
        int validCount,
        int errorCount,
        List<BulkRowError> errors,
        int emailNotRegisteredCount,
        PendingMasters newMasters
) {

    public static BulkValidateResult from(BulkAnalysis analysis) {
        return new BulkValidateResult(
                analysis.totalRows(), analysis.validCount(), analysis.errorCount(),
                analysis.errors(), analysis.emailNotRegisteredCount(), analysis.newMasters());
    }
}
