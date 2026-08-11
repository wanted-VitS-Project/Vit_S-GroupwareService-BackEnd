package com.group3.vitamins.bidding.bidnotice.application.result;

import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNotice;

import java.time.LocalDateTime;
import java.util.List;

// 직접 등록 또는 수정된 공고를 API 응답으로 전달할 결과입니다.
public record ManualBidNoticeResult(
        Long noticeId,
        String externalId,
        String noticeOrder,
        String sourceCode,
        String sourceName,
        String noticeStatus,
        String noticeName,
        String noticeType,
        String noticeAgency,
        LocalDateTime announcedAt,
        LocalDateTime bidDeadlineAt,
        List<Attachment> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // 저장된 직접 등록 공고를 API 결과 구조로 변환합니다.
    public static ManualBidNoticeResult from(ManualBidNotice notice) {
        return new ManualBidNoticeResult(
                notice.getNoticeId(),
                notice.getExternalId(),
                notice.getNoticeOrder(),
                ManualBidNotice.SOURCE_CODE,
                ManualBidNotice.SOURCE_NAME,
                notice.getNoticeStatus(),
                notice.getData().noticeName(),
                notice.getData().noticeType().name(),
                notice.getData().noticeAgency(),
                notice.getData().announcedAt(),
                notice.getData().bidDeadlineAt(),
                notice.getData().attachments().stream()
                        .map(Attachment::from)
                        .toList(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    // 직접 등록 공고의 공개 첨부 링크 응답입니다.
    public record Attachment(
            int attachmentOrder,
            String fileName,
            String sourceUrl
    ) {
        // 도메인 첨부 링크를 응답 항목으로 변환합니다.
        private static Attachment from(
                com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment attachment
        ) {
            return new Attachment(
                    attachment.attachmentOrder(),
                    attachment.fileName(),
                    attachment.sourceUrl()
            );
        }
    }
}
