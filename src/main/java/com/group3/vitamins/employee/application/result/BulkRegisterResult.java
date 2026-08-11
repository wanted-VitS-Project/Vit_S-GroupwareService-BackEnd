package com.group3.vitamins.employee.application.result;

import java.util.List;

/**
 * 엑셀 일괄 등록 <b>실행(§8)</b> 결과. 화면 ③ 결과의 카드 4개가 totalRows·registeredCount·failedCount·emailNotRegistered.size 다.
 *
 * @param errors             검증 오류 + 등록 중 실패(레이스 중복 등)를 합친 행별 오류
 * @param emailSentCount     초기 비밀번호 메일 발송 성공 건수
 * @param emailNotRegistered 등록됐지만 이메일이 없어 비밀번호를 못 받은 사원 (EMP-019)
 */
public record BulkRegisterResult(
        int totalRows,
        int registeredCount,
        int failedCount,
        List<BulkRowError> errors,
        int emailSentCount,
        List<BulkEmployeeRef> emailNotRegistered
) {
}
