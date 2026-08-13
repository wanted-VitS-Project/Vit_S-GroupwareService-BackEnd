package com.group3.vitamins.bidding.projectconversion.presentation.api.response;

import com.group3.vitamins.bidding.projectconversion.application.result.ConvertNoticeToProjectResult;

public record ConvertNoticeToProjectResponse(
        Long projectId
) {

    public static ConvertNoticeToProjectResponse from(ConvertNoticeToProjectResult result) {
        return new ConvertNoticeToProjectResponse(result.projectId());
    }
}
