package com.group3.vitamins.major.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전공 삭제 응답")
public record MajorDeleteResponse(
        @Schema(description = "삭제된 전공 번호") Long majorId
) {
}
