package com.group3.vitamins.employeegroup.presentation.api.response;

import com.group3.vitamins.employeegroup.application.result.AddMembersResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구성원 추가 결과(§6)")
public record AddMembersResponse(
        @Schema(description = "그룹 번호") Long groupId,
        @Schema(description = "요청 건수(중복 제거 후)") int requestedCount,
        @Schema(description = "새로 추가된 건수") int addedCount,
        @Schema(description = "이미 구성원이어서 건너뛴 건수") int alreadyMemberCount,
        @Schema(description = "처리 후 전체 구성원 수") int memberCount
) {

    public static AddMembersResponse from(AddMembersResult r) {
        return new AddMembersResponse(
                r.groupId(), r.requestedCount(), r.addedCount(), r.alreadyMemberCount(), r.memberCount());
    }
}
