package com.group3.vitamins.bidding.collectioncondition.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BiddingErrorCode implements ErrorCode {

    BIDDING_INVALID_COLLECTION_CONDITION(
            "BIDDING_INVALID_COLLECTION_CONDITION",
            "입찰 공고 수집 조건이 올바르지 않습니다."
    ),

    BIDDING_COLLECTION_QUERY_LIMIT_EXCEEDED(
            "BIDDING_COLLECTION_QUERY_LIMIT_EXCEEDED",
            "외부 API 호출 조합은 최대 20개까지 등록할 수 있습니다."
    ),

    BIDDING_UNSUPPORTED_SOURCE(
            "BIDDING_UNSUPPORTED_SOURCE",
            "지원하지 않거나 비활성화된 수집처입니다."
    ),

    BIDDING_COLLECTION_CONDITION_NOT_FOUND(
            "BIDDING_COLLECTION_CONDITION_NOT_FOUND",
            "입찰 공고 수집 조건을 찾을 수 없습니다."
    ),

    BIDDING_ACCESS_PERMISSION_REQUIRED(
            "BIDDING_ACCESS_PERMISSION_REQUIRED",
            "입찰 관리 권한이 필요합니다."
    ),
    BIDDING_INVALID_COLLECTION_RUN_REQUEST(
            "BIDDING_INVALID_COLLECTION_RUN_REQUEST",
            "입찰 공고 수집 실행 요청이 올바르지 않습니다."
    ),

    BIDDING_INACTIVE_COLLECTION_CONDITION(
            "BIDDING_INACTIVE_COLLECTION_CONDITION",
            "비활성화된 수집 조건은 실행할 수 없습니다."
    ),

    BIDDING_COLLECTION_RUN_ALREADY_PROCESSING(
            "BIDDING_COLLECTION_RUN_ALREADY_PROCESSING",
            "해당 수집 조건으로 진행 중인 실행이 있습니다."
    ),

    BIDDING_COLLECTION_RUN_NOT_FOUND(
            "BIDDING_COLLECTION_RUN_NOT_FOUND",
            "입찰 공고 수집 실행을 찾을 수 없습니다."
    ),

    BIDDING_INVALID_NOTICE_QUERY(
            "BIDDING_INVALID_NOTICE_QUERY",
            "입찰 공고 조회 조건이 올바르지 않습니다."
    ),

    BIDDING_NOTICE_NOT_FOUND(
            "BIDDING_NOTICE_NOT_FOUND",
            "입찰 공고를 찾을 수 없습니다."
    );

    private final String code;
    private final String message;
}
