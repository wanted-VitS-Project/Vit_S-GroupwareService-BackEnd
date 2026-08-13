package com.group3.vitamins.major.presentation.api.response;

import com.group3.vitamins.major.application.result.MajorListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "전공 마스터 목록 응답")
public record MajorListResponse(
        @Schema(description = "전공 목록") List<Item> majors
) {

    @Schema(description = "전공 항목")
    public record Item(
            @Schema(description = "전공 번호") Long majorId,
            @Schema(description = "전공명") String name,
            @Schema(description = "사용 사원 수(시스템·퇴사 제외)") int employeeCount,
            @Schema(description = "삭제 가능 여부(employeeCount == 0)") boolean deletable
    ) {
    }

    public static MajorListResponse from(List<MajorListItemResult> results) {
        List<Item> items = results.stream()
                .map(r -> new Item(r.majorId(), r.name(), r.employeeCount(), r.deletable()))
                .toList();
        return new MajorListResponse(items);
    }
}
