package com.group3.vitamins.bidding.bidnotice.application.result;

import java.util.List;

public record BidNoticeListResult(
        List<BidNoticeListItemResult> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
