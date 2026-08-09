package com.group3.vitamins.pagepermission.presentation.api.request;

import com.group3.vitamins.pagepermission.application.command.GrantPermissionsCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 페이지 권한 부여(§4) 요청 바디. permissions 는 1개 이상, 사번 중복 불가. */
@Schema(description = "페이지 권한 부여 요청 — 개인별 등급 배열(그룹 일괄도 같은 배열로)")
public record GrantPermissionsRequest(
        @Schema(description = "부여 대상 목록(1개 이상)") List<Item> permissions
) {
    @Schema(description = "부여 대상 1명")
    public record Item(
            @Schema(description = "사번") String userId,
            @Schema(description = "권한 등급 VIEWER·EDITOR") String permission
    ) {
    }

    /** 요청 → 커맨드. requesterRole·pageCode 는 인증·경로에서 채운다. */
    public GrantPermissionsCommand toCommand(String requesterRole, String pageCode) {
        List<GrantPermissionsCommand.Item> items = permissions == null ? null : permissions.stream()
                .map(i -> new GrantPermissionsCommand.Item(i.userId(), i.permission()))
                .toList();
        return new GrantPermissionsCommand(requesterRole, pageCode, items);
    }
}
