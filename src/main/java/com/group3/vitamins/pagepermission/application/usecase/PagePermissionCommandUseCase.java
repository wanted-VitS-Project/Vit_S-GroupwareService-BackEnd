package com.group3.vitamins.pagepermission.application.usecase;

import com.group3.vitamins.pagepermission.application.command.GrantPermissionsCommand;
import com.group3.vitamins.pagepermission.application.command.RevokePermissionCommand;
import com.group3.vitamins.pagepermission.application.result.GrantResult;
import com.group3.vitamins.pagepermission.application.result.RevokeResult;

/** 페이지 권한 변경 인바운드 포트 (§4 부여/등급변경 · §5 회수). 둘 다 ADMIN. */
public interface PagePermissionCommandUseCase {

    GrantResult grant(GrantPermissionsCommand command);

    RevokeResult revoke(RevokePermissionCommand command);
}
