package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewSourceQueryPort;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewSourcesQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewSourcesResult;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewSourcesResult.AttachmentSourceResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewSourcesUseCase;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BidReviewSourcesQueryService implements GetBidReviewSourcesUseCase {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "docx", "xlsx", "csv", "txt", "hwp", "hwpx"
    );

    private final BidReviewNoticeDocumentPort noticeDocumentPort;
    private final BidReviewSourceQueryPort sourceQueryPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public BidReviewSourcesResult get(GetBidReviewSourcesQuery query) {
        biddingAccessPolicy.assertAccess(query.userId(), query.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        noticeDocumentPort.findAccessibleNotice(companyId, query.noticeId())
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                ));

        List<AttachmentSourceResult> attachments = sourceQueryPort
                .findAttachmentSources(companyId, query.noticeId())
                .stream()
                .map(source -> new AttachmentSourceResult(
                        source.attachmentId(),
                        source.fileName(),
                        source.sourceType(),
                        isSupported(source.fileName())
                ))
                .toList();

        return new BidReviewSourcesResult(query.noticeId(), attachments);
    }

    private boolean isSupported(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return false;
        }

        String extension = fileName.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.contains(extension);
    }
}