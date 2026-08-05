package com.group3.vitamins.employee.application.usecase;

import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import com.group3.vitamins.employee.application.result.EmployeeRegisterResult;

/**
 * 사원 쓰기 유스케이스 (`employee.md` §3~§5) — 전부 ADMIN 전용.
 *
 * <p>PR-A 는 등록만 구현한다. 수정(§4)·퇴사(§5)는 PR-B 에서 이 인터페이스에 추가한다.
 */
public interface EmployeeCommandUseCase {

    /** 사원 등록 + 계정 발급(초기 비밀번호 메일). */
    EmployeeRegisterResult register(RegisterEmployeeCommand command);
}
