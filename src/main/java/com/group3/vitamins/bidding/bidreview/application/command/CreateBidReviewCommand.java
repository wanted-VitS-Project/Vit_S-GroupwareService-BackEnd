package com.group3.vitamins.bidding.bidreview.application.command;

import java.util.List;

public record CreateBidReviewCommand(
        Long noticeId,
        List<Long> bidAttachmentIds,
        List<Long> referenceFileIds,
        String prompt,
        String userId,
        String role
) {

    public CreateBidReviewCommand {
        bidAttachmentIds = bidAttachmentIds == null
                ? List.of()
                : List.copyOf(bidAttachmentIds);

        referenceFileIds = referenceFileIds == null
                ? List.of()
                : List.copyOf(referenceFileIds);
    }
}