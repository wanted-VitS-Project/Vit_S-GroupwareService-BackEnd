package com.group3.vitamins.employeegroup.presentation.api.response;

import com.group3.vitamins.employeegroup.application.result.GroupMembersResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "구성원 목록(§5)")
public record GroupMembersResponse(
        @Schema(description = "그룹 번호") Long groupId,
        @Schema(description = "그룹명") String name,
        @Schema(description = "구성원 목록(이름 오름차순)") List<Item> content
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Schema(name = "GroupMembersResponseItem", description = "구성원 항목")
    public record Item(
            @Schema(description = "사번") String userId,
            @Schema(description = "이름") String name,
            @Schema(description = "부서 경로 '기술본부 / 개발팀'(null 허용)") String departmentPath,
            @Schema(description = "직급명(null 허용)") String jobPositionName,
            @Schema(description = "추가일 yyyy-MM-dd") String addedAt
    ) {
    }

    public static GroupMembersResponse from(GroupMembersResult r) {
        List<Item> items = r.content().stream()
                .map(m -> new Item(m.userId(), m.name(), m.departmentPath(), m.jobPositionName(),
                        m.addedAt() == null ? null : m.addedAt().format(FMT)))
                .toList();
        return new GroupMembersResponse(r.groupId(), r.name(), items);
    }
}
