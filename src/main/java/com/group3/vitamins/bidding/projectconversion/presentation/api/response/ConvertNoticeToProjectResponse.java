package com.group3.vitamins.bidding.projectconversion.presentation.api.response;

import com.group3.vitamins.bidding.projectconversion.application.result.ConvertNoticeToProjectResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ConvertNoticeToProjectResponse(
        @Schema(description = "새로 생성된 프로젝트 ID", example = "1024")
        Long projectId
) {

    public static ConvertNoticeToProjectResponse from(ConvertNoticeToProjectResult result) {
        return new ConvertNoticeToProjectResponse(result.projectId());
    }
}
