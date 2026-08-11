package com.group3.vitamins.pagepermission.application.command;

import java.util.List;

/**
 * 페이지 권한 부여(§4) 커맨드. 부여와 등급 변경이 같은 요청이다(전체 교체 아님 — 목록에 없는 사용자는 건드리지 않음).
 * 그룹 일괄 적용도 프론트가 구성원을 같은 등급의 배열로 채워 보낸 것이라 개인 단위로 저장된다.
 */
public record GrantPermissionsCommand(
        String requesterRole,
        String pageCode,
        List<Item> permissions
) {
    /** 부여 대상 1명 — 사번 + 등급(VIEWER·EDITOR). */
    public record Item(String userId, String permission) {
    }
}
