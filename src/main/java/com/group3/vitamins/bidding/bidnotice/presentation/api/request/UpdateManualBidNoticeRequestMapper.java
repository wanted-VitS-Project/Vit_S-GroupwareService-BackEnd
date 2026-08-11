package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidnotice.application.command.PatchField;
import com.group3.vitamins.bidding.bidnotice.application.command.UpdateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

// PATCH JSON의 필드 생략과 명시적 null을 구분하여 Command로 변환합니다.
@Component
@RequiredArgsConstructor
public class UpdateManualBidNoticeRequestMapper {

    private final ObjectMapper objectMapper;

    // 직접 등록 공고 수정 JSON과 현재 인증 사용자를 Command로 변환합니다.
    public UpdateManualBidNoticeCommand toCommand(
            Long noticeId,
            JsonNode body,
            String userId,
            String role
    ) {
        if (body == null || !body.isObject()) {
            throw invalidRequest();
        }

        try {
            return new UpdateManualBidNoticeCommand(
                    noticeId,
                    field(body, "noticeName", String.class),
                    field(body, "noticeType", BidNoticeType.class),
                    field(body, "noticeAgency", String.class),
                    field(body, "demandAgency", String.class),
                    field(
                            body,
                            "internationalBidType",
                            InternationalBidType.class
                    ),
                    field(body, "announcedAt", LocalDateTime.class),
                    field(body, "bidStartAt", LocalDateTime.class),
                    field(body, "bidDeadlineAt", LocalDateTime.class),
                    field(body, "openingAt", LocalDateTime.class),
                    field(body, "baseAmount", BigDecimal.class),
                    field(body, "estimatedAmount", BigDecimal.class),
                    field(body, "bidMethod", String.class),
                    field(body, "contractMethod", String.class),
                    field(
                            body,
                            "participationQualificationText",
                            String.class
                    ),
                    field(body, "regionLimitText", String.class),
                    field(body, "businessLimitText", String.class),
                    field(body, "jointContractAllowed", Boolean.class),
                    field(body, "jointContractText", String.class),
                    field(body, "evaluationMethod", String.class),
                    field(body, "sourceUrl", String.class),
                    attachmentField(body),
                    userId,
                    role
            );
        } catch (IllegalArgumentException exception) {
            throw invalidRequest();
        }
    }

    // 일반 필드의 생략, null, 전달값을 PatchField로 구분합니다.
    private <T> PatchField<T> field(
            JsonNode body,
            String fieldName,
            Class<T> fieldType
    ) {
        if (!body.has(fieldName)) {
            return PatchField.absent();
        }

        JsonNode value = body.get(fieldName);
        if (value == null || value.isNull()) {
            return PatchField.of(null);
        }

        return PatchField.of(
                objectMapper.convertValue(value, fieldType)
        );
    }

    // 첨부 요청 배열을 요청 순서가 반영된 도메인 첨부 목록으로 변환합니다.
    private PatchField<List<ManualBidNoticeAttachment>> attachmentField(
            JsonNode body
    ) {
        if (!body.has("attachments")) {
            return PatchField.absent();
        }

        JsonNode value = body.get("attachments");
        if (value == null || value.isNull()) {
            throw invalidRequest();
        }

        List<ManualBidNoticeAttachmentRequest> requests =
                objectMapper.convertValue(
                        value,
                        new TypeReference<>() {
                        }
                );

        List<ManualBidNoticeAttachment> attachments =
                IntStream.range(0, requests.size())
                        .mapToObj(index -> requests.get(index)
                                .toDomain(index + 1))
                        .toList();

        return PatchField.of(attachments);
    }

    private ValidationException invalidRequest() {
        return new ValidationException(
                BiddingErrorCode.BIDDING_INVALID_MANUAL_NOTICE
        );
    }
}
