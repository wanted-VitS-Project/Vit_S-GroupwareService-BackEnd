package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCompanyDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewReferenceFilePort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewWorkerPort;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewJobQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewJobResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewJobUseCase;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.group3.vitamins.bidding.bidreview.application.support.BidReviewAttemptIdValidator.isValid;

@Service
@RequiredArgsConstructor
public class BidReviewJobQueryService implements GetBidReviewJobUseCase {

    private static final String BID_ATTACHMENT = "BID_ATTACHMENT";
    private static final String INTERNAL_REFERENCE = "INTERNAL_REFERENCE";
    private static final String COMPANY_DOCUMENT_REFERENCE = "COMPANY_DOCUMENT_REFERENCE";

    private final BidReviewWorkerPort workerPort;
    private final BidReviewNoticeDocumentPort noticeDocumentPort;
    private final BidReviewReferenceFilePort referenceFilePort;
    private final BidReviewCompanyDocumentPort companyDocumentPort;
    private final Clock clock;

    @Override
    @Transactional
    public BidReviewJobResult handle(GetBidReviewJobQuery query) {
        validate(query);

        BidReviewWorkerPort.ClaimedJob job = workerPort.claimJob(
                query.reviewId(),
                query.attemptId(),
                LocalDateTime.now(clock)
        ).orElseThrow(() -> new NotFoundException(
                BidReviewErrorCode.BIDDING_REVIEW_JOB_NOT_FOUND
        ));

        String noticeName = noticeDocumentPort
                .findAccessibleNotice(job.companyId(), job.noticeId())
                .map(BidReviewNoticeDocumentPort.NoticeSnapshot::noticeName)
                // 검토 생성 시점에 이미 접근 가능한 공고만 저장되므로 이론상 도달하지 않는다.
                // 방어적으로만 남겨둔다.
                .orElseThrow(() -> new NotFoundException(
                        BidReviewErrorCode.BIDDING_REVIEW_JOB_NOT_FOUND
                ));

        List<Long> attachmentIds = job.documents().stream()
                .filter(document -> BID_ATTACHMENT.equals(document.documentRole()))
                .map(BidReviewWorkerPort.JobDocument::bidAttachmentId)
                .toList();

        List<Long> referenceFileIds = job.documents().stream()
                .filter(document -> INTERNAL_REFERENCE.equals(document.documentRole()))
                .map(BidReviewWorkerPort.JobDocument::referenceFileId)
                .toList();

        List<Long> companyDocumentVersionIds = job.documents().stream()
                .filter(document -> COMPANY_DOCUMENT_REFERENCE.equals(document.documentRole()))
                .map(BidReviewWorkerPort.JobDocument::companyDocumentVersionId)
                .toList();

        Map<Long, BidReviewNoticeDocumentPort.AttachmentSnapshot> attachmentsById =
                noticeDocumentPort
                        .findAttachments(job.companyId(), job.noticeId(), attachmentIds)
                        .stream()
                        .collect(Collectors.toMap(
                                BidReviewNoticeDocumentPort.AttachmentSnapshot::attachmentId,
                                Function.identity()
                        ));

        Map<Long, BidReviewReferenceFilePort.DownloadableReferenceFile> referenceFilesById =
                referenceFilePort
                        .findDownloadableFiles(job.companyId(), referenceFileIds)
                        .stream()
                        .collect(Collectors.toMap(
                                BidReviewReferenceFilePort.DownloadableReferenceFile::referenceFileId,
                                Function.identity()
                        ));

        Map<Long, BidReviewCompanyDocumentPort.DownloadableCompanyDocument> companyDocumentsById =
                companyDocumentPort
                        .findDownloadableDocuments(job.companyId(), companyDocumentVersionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                BidReviewCompanyDocumentPort.DownloadableCompanyDocument::companyDocumentVersionId,
                                Function.identity()
                        ));

        List<BidReviewJobResult.AttachmentJob> attachments = attachmentIds.stream()
                .map(attachmentsById::get)
                .filter(Objects::nonNull)
                .map(attachment -> new BidReviewJobResult.AttachmentJob(
                        attachment.attachmentId(),
                        attachment.fileName(),
                        attachment.sourceUrl()
                ))
                .toList();

        List<BidReviewJobResult.ReferenceFileJob> referenceFiles = referenceFileIds.stream()
                .map(referenceFilesById::get)
                .filter(Objects::nonNull)
                .map(reference -> new BidReviewJobResult.ReferenceFileJob(
                        reference.referenceFileId(),
                        reference.fileName(),
                        reference.downloadUrl()
                ))
                .toList();

        List<BidReviewJobResult.CompanyDocumentJob> companyDocuments = companyDocumentVersionIds.stream()
                .map(companyDocumentsById::get)
                .filter(Objects::nonNull)
                .map(document -> new BidReviewJobResult.CompanyDocumentJob(
                        document.companyDocumentVersionId(),
                        document.fileName(),
                        document.downloadUrl()
                ))
                .toList();

        return new BidReviewJobResult(
                job.reviewId(),
                job.companyId(),
                job.attemptId(),
                job.prompt(),
                job.noticeId(),
                noticeName,
                attachments,
                referenceFiles,
                companyDocuments
        );
    }

    private void validate(GetBidReviewJobQuery query) {
        if (query == null
                || query.reviewId() == null
                || query.reviewId() <= 0
                || !isValid(query.attemptId())) {
            throw new ValidationException(
                    BidReviewErrorCode.BIDDING_INVALID_REVIEW_REQUEST
            );
        }
    }
}
