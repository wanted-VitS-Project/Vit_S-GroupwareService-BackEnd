package com.group3.vitamins.bidding.referencefile.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BidReferenceFileErrorCode implements ErrorCode {

    BIDDING_INVALID_REFERENCE_FILE_REQUEST(
            "BIDDING_INVALID_REFERENCE_FILE_REQUEST",
            "입찰 기준자료 요청값이 올바르지 않습니다."
    ),
    BIDDING_REFERENCE_FILE_NOT_FOUND(
            "BIDDING_REFERENCE_FILE_NOT_FOUND",
            "입찰 기준자료를 찾을 수 없습니다."
    ),
    BIDDING_REFERENCE_FILE_OBJECT_NOT_FOUND(
            "BIDDING_REFERENCE_FILE_OBJECT_NOT_FOUND",
            "업로드된 기준자료 객체를 찾을 수 없습니다."
    ),
    BIDDING_REFERENCE_FILE_SIZE_MISMATCH(
            "BIDDING_REFERENCE_FILE_SIZE_MISMATCH",
            "업로드된 기준자료 크기가 신고한 크기와 다릅니다."
    ),
    BIDDING_REFERENCE_FILE_IN_USE(
            "BIDDING_REFERENCE_FILE_IN_USE",
            "처리 중인 검토가 사용 중인 기준자료입니다."
    );

    private final String code;
    private final String message;
}