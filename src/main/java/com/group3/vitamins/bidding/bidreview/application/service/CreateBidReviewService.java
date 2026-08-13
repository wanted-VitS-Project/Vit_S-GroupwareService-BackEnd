package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.command.CreateBidReviewCommand;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCommandPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCompanyDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewReferenceFilePort;
import com.group3.vitamins.bidding.bidreview.application.result.CreateBidReviewResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.CreateBidReviewUseCase;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateBidReviewService implements CreateBidReviewUseCase {

    private static final int MAX_ATTACHMENT_COUNT = 10;
    private static final int MAX_REFERENCE_FILE_COUNT = 10;
    private static final int MAX_COMPANY_DOCUMENT_COUNT = 10;
    private static final int MAX_PROMPT_LENGTH = 3000;
    private static final String ACTIVE_PROCESSING_CONSTRAINT =
            "uk_bid_review_active_processing";

    private final BidReviewCommandPort commandPort;
    private final BidReviewNoticeDocumentPort noticeDocumentPort;
    private final BidReviewReferenceFilePort referenceFilePort;
    private final BidReviewCompanyDocumentPort companyDocumentPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    @Override
    public CreateBidReviewResult create(CreateBidReviewCommand command) {
        validateCommand(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();

        noticeDocumentPort.findAccessibleNotice(companyId, command.noticeId())
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                ));

        if (commandPort.existsProcessing(
                companyId,
                command.noticeId(),
                command.userId()
        )) {
            throw new ConflictException(
                    BidReviewErrorCode.BIDDING_REVIEW_ALREADY_PROCESSING
            );
        }

        List<BidReviewNoticeDocumentPort.AttachmentSnapshot> attachments =
                findAttachments(command, companyId);

        List<BidReviewReferenceFilePort.ReferenceFileSnapshot> references =
                findReferenceFiles(command, companyId);

        List<BidReviewCompanyDocumentPort.CompanyDocumentReferenceSnapshot> companyDocuments =
                findCompanyDocuments(command);

        LocalDateTime now = LocalDateTime.now(clock);
        String attemptId = UUID.randomUUID().toString();

        BidReview review = BidReview.createPending(
                companyId,
                command.noticeId(),
                command.userId(),
                command.prompt(),
                attemptId,
                now
        );

        List<BidReviewDocument> documents =
                createDocuments(attachments, references, companyDocuments, now);

        BidReview saved = savePending(review, documents);
        return CreateBidReviewResult.from(saved);
    }

    private List<BidReviewNoticeDocumentPort.AttachmentSnapshot> findAttachments(
            CreateBidReviewCommand command,
            Long companyId
    ) {
        List<BidReviewNoticeDocumentPort.AttachmentSnapshot> attachments =
                noticeDocumentPort.findAttachments(
                        companyId,
                        command.noticeId(),
                        command.bidAttachmentIds()
                );

        if (attachments.size() != command.bidAttachmentIds().size()) {
            throw new NotFoundException(
                    BidReviewErrorCode.BIDDING_NOTICE_ATTACHMENT_NOT_FOUND
            );
        }

        return attachments;
    }

    private List<BidReviewReferenceFilePort.ReferenceFileSnapshot> findReferenceFiles(
            CreateBidReviewCommand command,
            Long companyId
    ) {
        if (command.referenceFileIds().isEmpty()) {
            return List.of();
        }

        List<BidReviewReferenceFilePort.ReferenceFileSnapshot> references =
                referenceFilePort.findAccessibleFiles(
                        companyId,
                        command.referenceFileIds()
                );

        if (references.size() != command.referenceFileIds().size()) {
            throw new ForbiddenException(
                    BidReviewErrorCode.BIDDING_REVIEW_DOCUMENT_ACCESS_DENIED
            );
        }

        if (references.stream().anyMatch(reference -> !reference.isReady())) {
            throw new ConflictException(
                    BidReviewErrorCode.BIDDING_REVIEW_DOCUMENT_NOT_READY
            );
        }

        return references;
    }

    // 사내 문서함 참조 - CompanyDocumentReferenceUseCase가 이미 회사 스코프·완료 최신 버전만 노출하므로,
    // 반환 개수가 요청 개수와 다르면 다른 회사 문서거나 완료되지 않은 버전이다(둘을 구분할 근거가 없어
    // 기준자료 경로의 1단계 접근 거부와 동일하게 묶어서 403으로 처리한다).
    private List<BidReviewCompanyDocumentPort.CompanyDocumentReferenceSnapshot> findCompanyDocuments(
            CreateBidReviewCommand command
    ) {
        if (command.companyDocumentVersionIds().isEmpty()) {
            return List.of();
        }

        List<BidReviewCompanyDocumentPort.CompanyDocumentReferenceSnapshot> documents =
                companyDocumentPort.findAccessibleDocuments(
                        command.companyDocumentVersionIds()
                );

        if (documents.size() != command.companyDocumentVersionIds().size()) {
            throw new ForbiddenException(
                    BidReviewErrorCode.BIDDING_REVIEW_DOCUMENT_ACCESS_DENIED
            );
        }

        return documents;
    }

    private List<BidReviewDocument> createDocuments(
            List<BidReviewNoticeDocumentPort.AttachmentSnapshot> attachments,
            List<BidReviewReferenceFilePort.ReferenceFileSnapshot> references,
            List<BidReviewCompanyDocumentPort.CompanyDocumentReferenceSnapshot> companyDocuments,
            LocalDateTime now
    ) {
        List<BidReviewDocument> documents = new ArrayList<>();

        attachments.forEach(attachment ->
                documents.add(BidReviewDocument.createBidAttachment(
                        attachment.attachmentId(),
                        attachment.fileName(),
                        now
                ))
        );

        references.forEach(reference ->
                documents.add(BidReviewDocument.createInternalReference(
                        reference.referenceFileId(),
                        reference.fileName(),
                        now
                ))
        );

        companyDocuments.forEach(document ->
                documents.add(BidReviewDocument.createCompanyDocumentReference(
                        document.companyDocumentVersionId(),
                        document.fileName(),
                        now
                ))
        );

        return List.copyOf(documents);
    }

    private BidReview savePending(
            BidReview review,
            List<BidReviewDocument> documents
    ) {
        try {
            return commandPort.savePendingWithDocumentsAndOutbox(
                    review,
                    documents
            );
        } catch (DataIntegrityViolationException exception) {
            if (containsConstraint(exception, ACTIVE_PROCESSING_CONSTRAINT)) {
                throw new ConflictException(
                        BidReviewErrorCode.BIDDING_REVIEW_ALREADY_PROCESSING,
                        exception
                );
            }
            throw exception;
        }
    }

    private void validateCommand(CreateBidReviewCommand command) {
        if (command == null
                || command.noticeId() == null
                || command.noticeId() <= 0
                || command.userId() == null
                || command.userId().isBlank()
                || command.prompt() == null
                || command.prompt().isBlank()
                || command.prompt().codePointCount(
                0,
                command.prompt().length()
        ) > MAX_PROMPT_LENGTH
                || !validIds(
                command.bidAttachmentIds(),
                1,
                MAX_ATTACHMENT_COUNT
        )
                || !validIds(
                command.referenceFileIds(),
                0,
                MAX_REFERENCE_FILE_COUNT
        )
                || !validIds(
                command.companyDocumentVersionIds(),
                0,
                MAX_COMPANY_DOCUMENT_COUNT
        )) {
            throw new ValidationException(
                    BidReviewErrorCode.BIDDING_INVALID_REVIEW_REQUEST
            );
        }
    }

    private boolean validIds(
            List<Long> ids,
            int minimumSize,
            int maximumSize
    ) {
        return ids != null
                && ids.size() >= minimumSize
                && ids.size() <= maximumSize
                && ids.stream().allMatch(id -> id != null && id > 0)
                && new HashSet<>(ids).size() == ids.size();
    }

    private boolean containsConstraint(
            Throwable exception,
            String constraintName
    ) {
        Throwable current = exception;

        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains(constraintName)) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }
}