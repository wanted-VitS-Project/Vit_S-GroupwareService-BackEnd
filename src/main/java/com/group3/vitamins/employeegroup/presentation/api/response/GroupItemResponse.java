package com.group3.vitamins.employeegroup.presentation.api.response;

import com.group3.vitamins.employeegroup.application.result.GroupListRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;

@Schema(description = "그룹 목록 항목(§1) · 수정 응답(§3) 공통 구조")
public record GroupItemResponse(
        @Schema(description = "그룹 번호") Long groupId,
        @Schema(description = "그룹명") String name,
        @Schema(description = "설명(null 허용)") String description,
        @Schema(description = "구성원 수(시스템 계정·퇴사자 제외)") int memberCount,
        @Schema(description = "생성자 이름") String createdByName,
        @Schema(description = "생성일 yyyy-MM-dd") String createdAt
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static GroupItemResponse from(GroupListRow r) {
        return new GroupItemResponse(
                r.groupId(), r.name(), r.description(), r.memberCount(), r.createdByName(),
                r.createdAt() == null ? null : r.createdAt().format(FMT));
    }
}
