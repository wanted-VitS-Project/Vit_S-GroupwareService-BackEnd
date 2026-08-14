package com.group3.vitamins.bidding.projectconversion.application.command;

import java.time.LocalDate;
import java.util.List;

public record ConvertNoticeToProjectCommand(
        Long noticeId,
        Long reviewId,
        Long summaryId,
        String name,
        String description,
        Long businessCategoryId,
        LocalDate startedOn,
        LocalDate endedOn,
        List<String> memberIds,
        String requesterUserId,
        String role
) {

    public ConvertNoticeToProjectCommand {
        memberIds = memberIds == null ? List.of() : List.copyOf(memberIds);
    }
}
