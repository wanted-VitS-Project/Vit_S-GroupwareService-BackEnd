package com.group3.vitamins.bidding.bidnotice.application.command;

import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 직접 등록 공고의 부분 수정 여부와 전달값을 함께 보관합니다.
public record UpdateManualBidNoticeCommand(
        Long noticeId,
        PatchField<String> noticeName,
        PatchField<BidNoticeType> noticeType,
        PatchField<String> noticeAgency,
        PatchField<String> demandAgency,
        PatchField<InternationalBidType> internationalBidType,
        PatchField<LocalDateTime> announcedAt,
        PatchField<LocalDateTime> bidStartAt,
        PatchField<LocalDateTime> bidDeadlineAt,
        PatchField<LocalDateTime> openingAt,
        PatchField<BigDecimal> baseAmount,
        PatchField<BigDecimal> estimatedAmount,
        PatchField<String> bidMethod,
        PatchField<String> contractMethod,
        PatchField<String> participationQualificationText,
        PatchField<String> regionLimitText,
        PatchField<String> businessLimitText,
        PatchField<Boolean> jointContractAllowed,
        PatchField<String> jointContractText,
        PatchField<String> evaluationMethod,
        PatchField<String> sourceUrl,
        PatchField<List<ManualBidNoticeAttachment>> attachments,
        String userId,
        String role
) {
    public UpdateManualBidNoticeCommand {
        noticeName = normalize(noticeName);
        noticeType = normalize(noticeType);
        noticeAgency = normalize(noticeAgency);
        demandAgency = normalize(demandAgency);
        internationalBidType = normalize(internationalBidType);
        announcedAt = normalize(announcedAt);
        bidStartAt = normalize(bidStartAt);
        bidDeadlineAt = normalize(bidDeadlineAt);
        openingAt = normalize(openingAt);
        baseAmount = normalize(baseAmount);
        estimatedAmount = normalize(estimatedAmount);
        bidMethod = normalize(bidMethod);
        contractMethod = normalize(contractMethod);
        participationQualificationText = normalize(participationQualificationText);
        regionLimitText = normalize(regionLimitText);
        businessLimitText = normalize(businessLimitText);
        jointContractAllowed = normalize(jointContractAllowed);
        jointContractText = normalize(jointContractText);
        evaluationMethod = normalize(evaluationMethod);
        sourceUrl = normalize(sourceUrl);
        attachments = normalize(attachments);
    }

    // PATCH 요청에 변경할 필드가 하나 이상 포함됐는지 확인합니다.
    public boolean hasChanges() {
        return noticeName.present()
                || noticeType.present()
                || noticeAgency.present()
                || demandAgency.present()
                || internationalBidType.present()
                || announcedAt.present()
                || bidStartAt.present()
                || bidDeadlineAt.present()
                || openingAt.present()
                || baseAmount.present()
                || estimatedAmount.present()
                || bidMethod.present()
                || contractMethod.present()
                || participationQualificationText.present()
                || regionLimitText.present()
                || businessLimitText.present()
                || jointContractAllowed.present()
                || jointContractText.present()
                || evaluationMethod.present()
                || sourceUrl.present()
                || attachments.present();
    }

    // 변환 계층에서 누락 필드를 null로 넘겨도 PATCH 미전달 상태로 통일합니다.
    private static <T> PatchField<T> normalize(PatchField<T> field) {
        return field == null ? PatchField.absent() : field;
    }
}
