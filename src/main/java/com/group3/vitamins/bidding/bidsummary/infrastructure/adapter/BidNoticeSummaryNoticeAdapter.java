package com.group3.vitamins.bidding.bidsummary.infrastructure.adapter;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeQueryPort;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeDetailResult;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BidNoticeSummaryNoticeAdapter
        implements BidNoticeSummaryNoticePort {

    private final BidNoticeQueryPort bidNoticeQueryPort;

    // 기존 회사 격리 공고 조회 결과를 AI 요약용 스냅샷으로 변환합니다.
    @Override
    public Optional<BidNoticeSnapshot> findAccessibleNotice(
            Long companyId,
            Long noticeId
    ) {
        return bidNoticeQueryPort.findDetail(companyId, noticeId)
                .map(this::toSnapshot);
    }

    private BidNoticeSnapshot toSnapshot(BidNoticeDetailResult result) {
        return new BidNoticeSnapshot(
                result.noticeId(),
                result.noticeName(),
                result.noticeType(),
                result.noticeAgency(),
                result.demandAgency(),
                result.baseAmount(),
                result.estimatedAmount(),
                result.announcedAt(),
                result.bidStartAt(),
                result.bidDeadlineAt(),
                result.openingAt(),
                result.participationQualificationText(),
                result.regionLimitText(),
                result.businessLimitText(),
                result.contractMethod(),
                result.evaluationMethod(),
                result.sourceUrl(),
                toAttachments(result.attachments())
        );
    }

    private List<AttachmentSnapshot> toAttachments(
            List<BidNoticeDetailResult.Attachment> attachments
    ) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .map(attachment -> new AttachmentSnapshot(
                        attachment.attachmentOrder(),
                        attachment.fileName(),
                        attachment.sourceUrl()
                ))
                .toList();
    }
}