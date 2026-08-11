package com.group3.vitamins.bidding.bidnotice.application.service;

import com.group3.vitamins.bidding.bidnotice.application.command.CreateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.DismissBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.RestoreBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.UpdateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeCommandPort;
import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeStatusHistoryPort;
import com.group3.vitamins.bidding.bidnotice.application.port.CompanyBidNoticeStatePort;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeStatusResult;
import com.group3.vitamins.bidding.bidnotice.application.result.ManualBidNoticeResult;
import com.group3.vitamins.bidding.bidnotice.application.support.ManualBidNoticeDedupKeyGenerator;
import com.group3.vitamins.bidding.bidnotice.application.usecase.BidNoticeCommandUseCase;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNotice;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeData;
import com.group3.vitamins.bidding.bidnotice.domain.model.BidNoticeCompanyStatus;
import com.group3.vitamins.bidding.bidnotice.domain.model.BidNoticeStatusHistory;
import com.group3.vitamins.bidding.bidnotice.domain.model.CompanyBidNoticeState;
import com.group3.vitamins.bidding.bidnotice.domain.event.BidNoticeListChangedEvent;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// 회사 소유 직접 등록 공고의 생성과 부분 수정 흐름을 조율합니다.
@Service
@Transactional
@RequiredArgsConstructor
public class BidNoticeCommandService implements BidNoticeCommandUseCase {

    private static final int MAX_NOTICE_NAME_LENGTH = 1_000;
    private static final int MAX_AGENCY_LENGTH = 400;
    private static final int MAX_METHOD_LENGTH = 100;
    private static final int MAX_QUALIFICATION_LENGTH = 1_000;
    private static final int MAX_LIMIT_TEXT_LENGTH = 500;
    private static final int MAX_URL_LENGTH = 1_000;
    private static final int MAX_ATTACHMENT_COUNT = 10;
    private static final int MAX_ATTACHMENT_NAME_LENGTH = 255;

    private final BidNoticeCommandPort commandPort;
    private final CompanyBidNoticeStatePort companyStatePort;
    private final BidNoticeStatusHistoryPort statusHistoryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final ManualBidNoticeDedupKeyGenerator dedupKeyGenerator;
    private final DomainEventPublisher eventPublisher;

    // 입력값을 검증하고 현재 회사 소유의 직접 등록 공고를 생성합니다.
    @Override
    public ManualBidNoticeResult create(CreateManualBidNoticeCommand command) {
        validateCreateCommand(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        Long sourceId = commandPort.findManualSourceId()
                .orElseThrow(this::invalidManualNotice);
        LocalDateTime now = LocalDateTime.now();
        ManualBidNoticeData data = normalizeAndValidate(command.toData());
        String dedupKey = generateDedupKey(data);

        assertNotDuplicated(companyId, dedupKey, null);

        ManualBidNotice notice = ManualBidNotice.create(
                companyId,
                sourceId,
                "MANUAL-" + UUID.randomUUID(),
                dedupKey,
                data,
                command.userId(),
                now
        );

        ManualBidNotice saved = commandPort.save(notice);
        companyStatePort.observeManualRegistration(
                companyId,
                saved.getNoticeId(),
                now
        );
        publishListChanged(companyId);
        return ManualBidNoticeResult.from(saved);
    }

    // 현재 회사가 직접 등록한 공고에 전달된 PATCH 필드만 반영합니다.
    @Override
    public ManualBidNoticeResult update(UpdateManualBidNoticeCommand command) {
        validateUpdateCommand(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        ManualBidNotice notice = findEditableNotice(companyId, command.noticeId());
        ManualBidNoticeData merged = normalizeAndValidate(merge(notice.getData(), command));
        String dedupKey = generateDedupKey(merged);

        assertNotDuplicated(companyId, dedupKey, notice.getNoticeId());
        notice.update(merged, dedupKey, LocalDateTime.now());

        ManualBidNotice saved = commandPort.save(notice);
        publishListChanged(companyId);
        return ManualBidNoticeResult.from(saved);
    }

    @Override
    public BidNoticeStatusResult dismiss(DismissBidNoticeCommand command) {
        validateDismissCommand(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        CompanyBidNoticeState current = findCompanyState(companyId, command.noticeId());
        if (current.status() == BidNoticeCompanyStatus.DISMISSED) {
            throw new ConflictException(BiddingErrorCode.BIDDING_NOTICE_ALREADY_DISMISSED);
        }

        LocalDateTime now = LocalDateTime.now();
        String reason = command.reason().trim();
        CompanyBidNoticeState changed = current.dismiss(reason, now);
        companyStatePort.update(changed);
        saveStatusHistory(current, changed, reason, command.userId(), now);
        publishListChanged(companyId);
        return toStatusResult(changed);
    }

    @Override
    public BidNoticeStatusResult restore(RestoreBidNoticeCommand command) {
        validateRestoreCommand(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        CompanyBidNoticeState current = findCompanyState(companyId, command.noticeId());
        if (current.status() != BidNoticeCompanyStatus.DISMISSED) {
            throw new ConflictException(BiddingErrorCode.BIDDING_NOTICE_NOT_DISMISSED);
        }

        LocalDateTime now = LocalDateTime.now();
        CompanyBidNoticeState changed = current.restore(now);
        companyStatePort.update(changed);
        saveStatusHistory(current, changed, null, command.userId(), now);
        publishListChanged(companyId);
        return toStatusResult(changed);
    }

    private void publishListChanged(Long companyId) {
        eventPublisher.publish(new BidNoticeListChangedEvent(companyId));
    }

    private CompanyBidNoticeState findCompanyState(Long companyId, Long noticeId) {
        return companyStatePort.findForUpdate(companyId, noticeId)
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                ));
    }

    private void saveStatusHistory(
            CompanyBidNoticeState previous,
            CompanyBidNoticeState changed,
            String reason,
            String userId,
            LocalDateTime now
    ) {
        statusHistoryPort.save(new BidNoticeStatusHistory(
                changed.companyId(), changed.noticeId(), previous.status(),
                changed.status(), reason, userId, now
        ));
    }

    private BidNoticeStatusResult toStatusResult(CompanyBidNoticeState state) {
        return new BidNoticeStatusResult(
                state.noticeId(), state.status().name(),
                state.dismissReason(), state.updatedAt()
        );
    }

    private void validateDismissCommand(DismissBidNoticeCommand command) {
        if (command == null || command.noticeId() == null || command.noticeId() <= 0
                || isBlank(command.userId()) || isBlank(command.reason())
                || command.reason().trim().length() > 500) {
            throw new ValidationException(BiddingErrorCode.BIDDING_INVALID_DISMISS_REASON);
        }
    }

    private void validateRestoreCommand(RestoreBidNoticeCommand command) {
        if (command == null || command.noticeId() == null || command.noticeId() <= 0
                || isBlank(command.userId())) {
            throw new ValidationException(BiddingErrorCode.BIDDING_INVALID_NOTICE_QUERY);
        }
    }

    // 현재 회사 소유 공고와 공용 외부 공고를 구분하여 명세에 맞는 오류를 반환합니다.
    private ManualBidNotice findEditableNotice(Long companyId, Long noticeId) {
        return commandPort.findOwnedManualNotice(companyId, noticeId)
                .orElseThrow(() -> {
                    if (commandPort.existsExternalNotice(noticeId)) {
                        return new ConflictException(
                                BiddingErrorCode.BIDDING_NOTICE_EDIT_NOT_ALLOWED
                        );
                    }
                    return new NotFoundException(
                            BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                    );
                });
    }

    // 기존 공고와 PATCH 전달값을 병합하며 명시적 null은 그대로 해제 값으로 반영합니다.
    private ManualBidNoticeData merge(
            ManualBidNoticeData current,
            UpdateManualBidNoticeCommand command
    ) {
        return new ManualBidNoticeData(
                command.noticeName().resolve(current.noticeName()),
                command.noticeType().resolve(current.noticeType()),
                command.noticeAgency().resolve(current.noticeAgency()),
                command.demandAgency().resolve(current.demandAgency()),
                command.internationalBidType().resolve(current.internationalBidType()),
                command.announcedAt().resolve(current.announcedAt()),
                command.bidStartAt().resolve(current.bidStartAt()),
                command.bidDeadlineAt().resolve(current.bidDeadlineAt()),
                command.openingAt().resolve(current.openingAt()),
                command.baseAmount().resolve(current.baseAmount()),
                command.estimatedAmount().resolve(current.estimatedAmount()),
                command.bidMethod().resolve(current.bidMethod()),
                command.contractMethod().resolve(current.contractMethod()),
                command.participationQualificationText()
                        .resolve(current.participationQualificationText()),
                command.regionLimitText().resolve(current.regionLimitText()),
                command.businessLimitText().resolve(current.businessLimitText()),
                command.jointContractAllowed().resolve(current.jointContractAllowed()),
                command.jointContractText().resolve(current.jointContractText()),
                command.evaluationMethod().resolve(current.evaluationMethod()),
                command.sourceUrl().resolve(current.sourceUrl()),
                command.attachments().resolve(current.attachments())
        );
    }

    // 등록 요청의 기본 식별값이 누락되면 공고 내용 검증 전에 거부합니다.
    private void validateCreateCommand(CreateManualBidNoticeCommand command) {
        if (command == null || isBlank(command.userId())) {
            throw invalidManualNotice();
        }
    }

    // 수정 대상과 변경 필드가 없는 PATCH 요청을 저장소 접근 전에 거부합니다.
    private void validateUpdateCommand(UpdateManualBidNoticeCommand command) {
        if (command == null
                || command.noticeId() == null
                || command.noticeId() <= 0
                || isBlank(command.userId())
                || !command.hasChanges()) {
            throw invalidManualNotice();
        }
    }

    // 등록과 수정에 공통인 필드 규칙을 검증하고 저장용 값으로 정규화합니다.
    private ManualBidNoticeData normalizeAndValidate(ManualBidNoticeData data) {
        if (data == null
                || isInvalidRequired(data.noticeName(), MAX_NOTICE_NAME_LENGTH)
                || data.noticeType() == null
                || isInvalidRequired(data.noticeAgency(), MAX_AGENCY_LENGTH)
                || data.announcedAt() == null
                || data.bidDeadlineAt() == null
                || !data.bidDeadlineAt().isAfter(data.announcedAt())
                || isInvalidOptional(data.demandAgency(), MAX_AGENCY_LENGTH)
                || isInvalidOptional(data.bidMethod(), MAX_METHOD_LENGTH)
                || isInvalidOptional(data.contractMethod(), MAX_METHOD_LENGTH)
                || isInvalidOptional(
                        data.participationQualificationText(),
                        MAX_QUALIFICATION_LENGTH
                )
                || isInvalidOptional(data.regionLimitText(), MAX_LIMIT_TEXT_LENGTH)
                || isInvalidOptional(data.businessLimitText(), MAX_LIMIT_TEXT_LENGTH)
                || isInvalidOptional(data.jointContractText(), MAX_LIMIT_TEXT_LENGTH)
                || isInvalidOptional(data.evaluationMethod(), MAX_METHOD_LENGTH)
                || isNegative(data.baseAmount())
                || isNegative(data.estimatedAmount())
                || !isValidOptionalUrl(data.sourceUrl())
                || !areValidAttachments(data.attachments())) {
            throw invalidManualNotice();
        }

        return new ManualBidNoticeData(
                data.noticeName().trim(),
                data.noticeType(),
                data.noticeAgency().trim(),
                trimToNull(data.demandAgency()),
                data.internationalBidType(),
                data.announcedAt(),
                data.bidStartAt(),
                data.bidDeadlineAt(),
                data.openingAt(),
                data.baseAmount(),
                data.estimatedAmount(),
                trimToNull(data.bidMethod()),
                trimToNull(data.contractMethod()),
                trimToNull(data.participationQualificationText()),
                trimToNull(data.regionLimitText()),
                trimToNull(data.businessLimitText()),
                data.jointContractAllowed(),
                trimToNull(data.jointContractText()),
                trimToNull(data.evaluationMethod()),
                trimToNull(data.sourceUrl()),
                normalizeAttachments(data.attachments())
        );
    }

    // 요청 배열의 순서를 첨부 순번으로 다시 부여합니다.
    private List<ManualBidNoticeAttachment> normalizeAttachments(
            List<ManualBidNoticeAttachment> attachments
    ) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return java.util.stream.IntStream.range(0, attachments.size())
                .mapToObj(index -> new ManualBidNoticeAttachment(
                        index + 1,
                        attachments.get(index).fileName().trim(),
                        attachments.get(index).sourceUrl().trim()
                ))
                .toList();
    }

    // 첨부 개수, 파일명, 공개 URL, 요청 내 URL 중복을 확인합니다.
    private boolean areValidAttachments(List<ManualBidNoticeAttachment> attachments) {
        if (attachments == null || attachments.size() > MAX_ATTACHMENT_COUNT) {
            return false;
        }

        Set<String> urls = new HashSet<>();
        for (ManualBidNoticeAttachment attachment : attachments) {
            if (attachment == null
                    || isInvalidRequired(attachment.fileName(), MAX_ATTACHMENT_NAME_LENGTH)
                    || !isValidUrl(attachment.sourceUrl())
                    || !urls.add(attachment.sourceUrl().trim())) {
                return false;
            }
        }
        return true;
    }

    // http 또는 https 공개 URL 형식이며 인증 정보가 포함되지 않았는지 확인합니다.
    private boolean isValidOptionalUrl(String value) {
        return value == null || isValidUrl(value);
    }

    private boolean isValidUrl(String value) {
        if (isBlank(value) || value.length() > MAX_URL_LENGTH) {
            return false;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            return scheme != null
                    && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && !isBlank(uri.getHost())
                    && uri.getUserInfo() == null
                    && !containsSensitiveQuery(uri.getRawQuery());
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    // 서명·토큰이 포함된 임시 URL이 영구 저장되는 것을 막습니다.
    private boolean containsSensitiveQuery(String query) {
        if (query == null) {
            return false;
        }
        String lower = query.toLowerCase(Locale.ROOT);
        return lower.contains("token=")
                || lower.contains("signature=")
                || lower.contains("x-amz-")
                || lower.contains("credential=");
    }

    // 애플리케이션 사전 검사와 DB UNIQUE 제약의 기준이 같은 중복 키를 사용합니다.
    private String generateDedupKey(ManualBidNoticeData data) {
        return dedupKeyGenerator.generate(
                data.noticeName(),
                data.noticeAgency(),
                data.announcedAt(),
                data.bidDeadlineAt()
        );
    }

    private void assertNotDuplicated(
            Long companyId,
            String dedupKey,
            Long excludedNoticeId
    ) {
        if (commandPort.existsActiveDuplicate(
                companyId,
                dedupKey,
                excludedNoticeId
        )) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_MANUAL_NOTICE_DUPLICATED
            );
        }
    }

    private boolean isInvalidRequired(String value, int maxLength) {
        return isBlank(value) || value.length() > maxLength;
    }

    private boolean isInvalidOptional(String value, int maxLength) {
        return value != null && (value.isBlank() || value.length() > maxLength);
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        return value == null ? null : value.trim();
    }

    private ValidationException invalidManualNotice() {
        return new ValidationException(
                BiddingErrorCode.BIDDING_INVALID_MANUAL_NOTICE
        );
    }
}
