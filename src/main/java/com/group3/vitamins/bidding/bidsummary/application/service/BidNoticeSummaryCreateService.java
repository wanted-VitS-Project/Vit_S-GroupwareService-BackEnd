package com.group3.vitamins.bidding.bidsummary.application.service;

import com.group3.vitamins.bidding.bidsummary.application.command.CreateBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryCommandPort;
import com.group3.vitamins.bidding.bidsummary.application.port.BidNoticeSummaryNoticePort;
import com.group3.vitamins.bidding.bidsummary.application.result.CreateBidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.CreateBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummary;
import com.group3.vitamins.bidding.bidsummary.domain.model.BidNoticeSummaryStatus;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class BidNoticeSummaryCreateService
        implements CreateBidNoticeSummaryUseCase {

    private static final int MAX_PROMPT_LENGTH = 3000;
    private static final int MAX_REVISION_NO = 20;
    private static final String ACTIVE_PROCESSING_CONSTRAINT =
            "uk_bid_notice_summary_active_processing";

    private final BidNoticeSummaryNoticePort noticePort;
    private final BidNoticeSummaryCommandPort commandPort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    // 현재 회사에서 접근 가능한 공고에 새로운 AI 요약을 요청합니다.
    @Override
    public CreateBidNoticeSummaryResult create(
            CreateBidNoticeSummaryCommand command
    ) {
        validateCommand(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();

        BidNoticeSummaryNoticePort.BidNoticeSnapshot noticeSnapshot =
                noticePort.findAccessibleNotice(companyId, command.noticeId())
                        .orElseThrow(() -> new NotFoundException(
                                BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                        ));

        if (commandPort.existsInProgress(
                companyId,
                command.noticeId(),
                command.userId()
        )) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_SUMMARY_ALREADY_PROCESSING
            );
        }

        LocalDateTime now = LocalDateTime.now(clock);
        BidNoticeSummaryCommandPort.ImprovementBase base =
                findAndValidateBase(command, companyId);

        BidNoticeSummary summary = BidNoticeSummary.createPending(
                companyId,
                command.noticeId(),
                command.userId(),
                command.prompt(),
                base == null ? null : base.summaryId(),
                base == null ? 1 : base.revisionNo() + 1,
                UUID.randomUUID().toString(),
                now
        );

        BidNoticeSummary saved = savePending(summary, noticeSnapshot);

        return new CreateBidNoticeSummaryResult(
                saved.summaryId(),
                saved.summaryStatus().name(),
                saved.createdAt()
        );
    }

    private BidNoticeSummary savePending(
            BidNoticeSummary summary,
            BidNoticeSummaryNoticePort.BidNoticeSnapshot noticeSnapshot
    ) {
        try {
            return commandPort.savePendingWithOutbox(summary, noticeSnapshot);
        } catch (DataIntegrityViolationException exception) {
            if (isActiveProcessingConstraintViolation(exception)) {
                throw new ConflictException(
                        BiddingErrorCode.BIDDING_SUMMARY_ALREADY_PROCESSING,
                        exception
                );
            }
            throw exception;
        }
    }

    private boolean isActiveProcessingConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && message.contains(ACTIVE_PROCESSING_CONSTRAINT)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private BidNoticeSummaryCommandPort.ImprovementBase findAndValidateBase(
            CreateBidNoticeSummaryCommand command,
            Long companyId
    ) {
        if (command.baseSummaryId() == null) {
            return null;
        }

        BidNoticeSummaryCommandPort.ImprovementBase base = commandPort
                .findImprovementBaseForUpdate(
                        companyId,
                        command.noticeId(),
                        command.userId(),
                        command.baseSummaryId()
                )
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND
                ));

        if (base.summaryStatus() != BidNoticeSummaryStatus.COMPLETED
                || base.confirmed()
                || base.revisionNo() >= MAX_REVISION_NO) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_SUMMARY_NOT_EDITABLE
            );
        }

        return base;
    }

    // 공고 ID와 사용자 프롬프트 형식을 저장소 접근 전에 검증합니다.
    private void validateCommand(CreateBidNoticeSummaryCommand command) {
        if (command == null
                || command.noticeId() == null
                || command.noticeId() <= 0
                || (command.baseSummaryId() != null
                && command.baseSummaryId() <= 0)
                || command.userId() == null
                || command.userId().isBlank()
                || command.prompt() == null
                || command.prompt().isBlank()
                || command.prompt().codePointCount(
                0,
                command.prompt().length()
        ) > MAX_PROMPT_LENGTH) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_INVALID_SUMMARY_REQUEST
            );
        }
    }
}
