package com.group3.vitamins.employeegroup.presentation.api.response;

import com.group3.vitamins.employeegroup.application.result.GroupCreateResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "그룹 생성(§2) 응답 — 갓 만든 그룹이라 memberCount 는 0")
public record GroupCreateResponse(
        @Schema(description = "그룹 번호") Long groupId,
        @Schema(description = "그룹명") String name,
        @Schema(description = "설명(null 허용)") String description,
        @Schema(description = "구성원 수(0)") int memberCount
) {

    public static GroupCreateResponse from(GroupCreateResult r) {
        return new GroupCreateResponse(r.groupId(), r.name(), r.description(), r.memberCount());
    }
}
