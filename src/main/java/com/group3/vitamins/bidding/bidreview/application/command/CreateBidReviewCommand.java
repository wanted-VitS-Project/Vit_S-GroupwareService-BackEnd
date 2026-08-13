package com.group3.vitamins.bidding.bidreview.application.command;

import java.util.List;

public record CreateBidReviewCommand(
        Long noticeId,
        List<Long> bidAttachmentIds,
        List<Long> referenceFileIds,
        List<Long> companyDocumentVersionIds,
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

        companyDocumentVersionIds = companyDocumentVersionIds == null
                ? List.of()
                : List.copyOf(companyDocumentVersionIds);
    }
}