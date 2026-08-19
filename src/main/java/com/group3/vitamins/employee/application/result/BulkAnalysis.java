package com.group3.vitamins.employee.application.result;

import java.util.List;

/**
 * 파싱된 엑셀을 검증한 <b>내부 분석 결과</b> — 검증(§7)과 일괄 등록(§8)이 공유한다.
 * 검증은 이 값을 요약해 돌려주고, 등록은 {@code validRows} 를 그대로 등록에 태운다(같은 판정을 두 번 하지 않는다).
 *
 * @param emailNotRegisteredCount 등록 가능한 행 중 이메일이 없는 행 수(EMP-019) — 등록돼도 초기 비밀번호를 못 받는다.
 * @param newMasters              {@code autoCreateMasters=true} 일 때 유효 행이 참조하는, 마스터에 없는 전공/자격증(등록 시 생성 대상). 아니면 빈 값.
 */
public record BulkAnalysis(
        int totalRows,
        List<ResolvedEmployeeRow> validRows,
        List<BulkRowError> errors,
        int emailNotRegisteredCount,
        PendingMasters newMasters
) {

    public int validCount() {
        return validRows.size();
    }

    public int errorCount() {
        return errors.size();
    }
}
