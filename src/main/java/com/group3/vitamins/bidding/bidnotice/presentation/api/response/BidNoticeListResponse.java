package com.group3.vitamins.bidding.bidnotice.presentation.api.response;

import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListItemResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "입찰 공고 목록 응답")
public record BidNoticeListResponse(
        List<Item> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    @Schema(name = "BidNoticeListResponseItem")
    public record Item(
            Long noticeId, String noticeName, String sourceCode, String sourceName,
            String sourceUrl, String noticeAgency, Long businessCategoryId,
            String businessCategoryName, BigDecimal baseAmount, BigDecimal estimatedAmount,
            LocalDateTime announcedAt, LocalDateTime bidDeadlineAt, Integer dDay,
            boolean isNew, String noticeStatus,
            @Schema(description = "현재 회사 공용 관심 등록 여부", example = "false") boolean isFavorite,
            Long projectId
    ) {
        static Item from(BidNoticeListItemResult result) {
            return new Item(
                    result.noticeId(), result.noticeName(), result.sourceCode(), result.sourceName(),
                    result.sourceUrl(), result.noticeAgency(), result.businessCategoryId(),
                    result.businessCategoryName(), result.baseAmount(), result.estimatedAmount(),
                    result.announcedAt(), result.bidDeadlineAt(), result.dDay(), result.isNew(),
                    result.noticeStatus(), result.isFavorite(), result.projectId()
            );
        }
    }

    public static BidNoticeListResponse from(BidNoticeListResult result) {
        return new BidNoticeListResponse(
                result.content().stream().map(Item::from).toList(),
                result.totalElements(), result.totalPages(), result.page(), result.size()
        );
    }
}
