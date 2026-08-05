package com.group3.vitamins.account.application.usecase;

import com.group3.vitamins.account.application.command.ChangeRoleCommand;
import com.group3.vitamins.account.application.command.ChangeStatusCommand;

/**
 * 계정 관리 인바운드 포트 — 전역 권한 변경 · 계정 상태 변경 (`.ai/api/account.md` §1·§2). 둘 다 ADMIN 전용.
 *
 * <p>응답 데이터가 입력값(사번·변경값)과 동일하므로 반환값을 두지 않는다 — 프레젠테이션이 요청값으로 응답을 구성한다.
 */
public interface AccountCommandUseCase {

    void changeRole(ChangeRoleCommand command);

    void changeStatus(ChangeStatusCommand command);
}
