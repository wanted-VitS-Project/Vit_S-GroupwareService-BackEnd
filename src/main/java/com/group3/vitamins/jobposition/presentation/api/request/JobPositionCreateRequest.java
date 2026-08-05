package com.group3.vitamins.jobposition.presentation.api.request;

import com.group3.vitamins.jobposition.application.command.CreateJobPositionCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직급 생성 요청")
public record JobPositionCreateRequest(

        @Schema(description = "직급명 (필수, 최대 30자, 중복 불가)", example = "대리")
        String name,

        @Schema(description = "정렬 순서 (선택). 생략하면 마지막 순서 + 1", example = "2")
        Integer sortOrder
) {

    /** 요청을 서비스 커맨드로 옮긴다. role 은 세션에서 가져온 값이라 요청 바디에 없다. */
    public CreateJobPositionCommand toCommand(String role) {
        return new CreateJobPositionCommand(name, sortOrder, role);
    }
}
