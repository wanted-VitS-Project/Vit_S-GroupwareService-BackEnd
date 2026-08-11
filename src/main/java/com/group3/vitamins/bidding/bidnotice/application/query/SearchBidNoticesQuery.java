package com.group3.vitamins.bidding.bidnotice.application.query;

import java.time.LocalDate;

public record SearchBidNoticesQuery(
        LocalDate startDate, LocalDate endDate, String noticeAgency,
        Long businessCategoryId, String region, Boolean deadlineSoon,
        String keyword, String noticeStatus, String sort,
        int page, int size, String userId, String role
) {
}
