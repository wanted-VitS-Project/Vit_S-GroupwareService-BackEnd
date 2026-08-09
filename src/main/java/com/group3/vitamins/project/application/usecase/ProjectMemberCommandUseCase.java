package com.group3.vitamins.project.application.usecase;

import com.group3.vitamins.project.application.command.AddMemberCommand;
import com.group3.vitamins.project.application.command.ChangeMemberPermissionCommand;
import com.group3.vitamins.project.application.command.RemoveMemberCommand;
import com.group3.vitamins.project.application.result.MemberResult;

public interface ProjectMemberCommandUseCase {

    MemberResult addMember(AddMemberCommand command);

    MemberResult changePermission(ChangeMemberPermissionCommand command);

    void removeMember(RemoveMemberCommand command);
}