package com.group3.vitamins.major.presentation.api.response;

import com.group3.vitamins.major.application.result.MajorResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전공 생성·수정 응답")
public record MajorResponse(
        @Schema(description = "전공 번호") Long majorId,
        @Schema(description = "전공명") String name
) {

    public static MajorResponse from(MajorResult r) {
        return new MajorResponse(r.majorId(), r.name());
    }
}
