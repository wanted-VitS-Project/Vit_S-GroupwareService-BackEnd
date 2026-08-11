package com.group3.vitamins.employeegroup.application.usecase;

import com.group3.vitamins.employeegroup.application.command.AddMembersCommand;
import com.group3.vitamins.employeegroup.application.command.CreateGroupCommand;
import com.group3.vitamins.employeegroup.application.command.DeleteGroupCommand;
import com.group3.vitamins.employeegroup.application.command.RemoveMemberCommand;
import com.group3.vitamins.employeegroup.application.command.UpdateGroupCommand;
import com.group3.vitamins.employeegroup.application.result.AddMembersResult;
import com.group3.vitamins.employeegroup.application.result.GroupCreateResult;
import com.group3.vitamins.employeegroup.application.result.RemoveMemberResult;

/** 그룹 생성·수정·삭제 인바운드 포트 (§2·§3·§4). 전부 ADMIN 전용. */
public interface EmployeeGroupCommandUseCase {

    GroupCreateResult create(CreateGroupCommand command);

    /** 이름·설명을 수정한다. 응답(목록 구조)은 컨트롤러가 조회로 다시 읽는다. */
    void update(UpdateGroupCommand command);

    void delete(DeleteGroupCommand command);

    /** 구성원 다건 추가(§6) — 멱등. 없는 사번이 있으면 전체 거부. */
    AddMembersResult addMembers(AddMembersCommand command);

    /** 구성원 단건 제거(§7). */
    RemoveMemberResult removeMember(RemoveMemberCommand command);
}
