package com.group3.vitamins.employeegroup.presentation.api.response;

import com.group3.vitamins.employeegroup.application.result.RemoveMemberResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구성원 제거 결과(§7)")
public record RemoveMemberResponse(
        @Schema(description = "그룹 번호") Long groupId,
        @Schema(description = "처리 후 전체 구성원 수") int memberCount
) {

    public static RemoveMemberResponse from(RemoveMemberResult r) {
        return new RemoveMemberResponse(r.groupId(), r.memberCount());
    }
}
