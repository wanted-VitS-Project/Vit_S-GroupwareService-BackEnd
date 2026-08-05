package com.group3.vitamins.employee.application.usecase;

import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import com.group3.vitamins.employee.application.command.ResignEmployeeCommand;
import com.group3.vitamins.employee.application.command.UpdateEmployeeCommand;
import com.group3.vitamins.employee.application.result.EmployeeRegisterResult;
import com.group3.vitamins.employee.application.result.EmployeeResignResult;

/**
 * 사원 쓰기 유스케이스 (`employee.md` §3~§5) — 전부 ADMIN 전용.
 */
public interface EmployeeCommandUseCase {

    /** 사원 등록 + 계정 발급(초기 비밀번호 메일). */
    EmployeeRegisterResult register(RegisterEmployeeCommand command);

    /** 사원 정보 수정 (전달한 필드만). 응답 상세는 호출부가 조회 유스케이스로 다시 읽는다. */
    void updateEmployee(UpdateEmployeeCommand command);

    /** 퇴사 처리 — 퇴사일 기록 + 계정 비활성화. */
    EmployeeResignResult resignEmployee(ResignEmployeeCommand command);
}
