package com.group3.vitamins.employee.application.usecase;

import com.group3.vitamins.employee.application.command.RegisterBulkCommand;
import com.group3.vitamins.employee.application.command.ValidateBulkCommand;
import com.group3.vitamins.employee.application.result.BulkRegisterResult;
import com.group3.vitamins.employee.application.result.BulkValidateResult;

/**
 * 사원 엑셀 일괄 등록 인바운드 포트 (employee.md §6~§8). 전부 ADMIN 전용이다.
 * 템플릿 → 검증 → 등록 순으로 메서드를 더한다.
 */
public interface EmployeeBulkUseCase {

    /** 일괄 등록 템플릿(.xlsx) 바이너리를 돌려준다 (§6). */
    byte[] getTemplate(String actorRole);

    /** 업로드 엑셀을 검증한다 (§7) — 등록하지 않고 행별 오류만 돌려준다(오류가 있어도 성공). */
    BulkValidateResult validate(ValidateBulkCommand command);

    /** 업로드 엑셀로 사원을 일괄 등록한다 (§8). skipErrors=false 면 오류가 있을 때 등록하지 않는다(부분 등록 허용). */
    BulkRegisterResult register(RegisterBulkCommand command);
}
