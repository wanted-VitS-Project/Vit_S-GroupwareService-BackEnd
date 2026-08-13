package com.group3.vitamins.bidding.bidreview.domain.exception;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BidReviewErrorCode implements ErrorCode {

    BIDDING_INVALID_REVIEW_REQUEST(
            "BIDDING_INVALID_REVIEW_REQUEST",
            "입찰 문서 검토 요청값이 올바르지 않습니다."
    ),
    BIDDING_REVIEW_ALREADY_PROCESSING(
            "BIDDING_REVIEW_ALREADY_PROCESSING",
            "현재 처리 중인 입찰 문서 검토가 있습니다."
    ),
    BIDDING_REVIEW_DOCUMENT_ACCESS_DENIED(
            "BIDDING_REVIEW_DOCUMENT_ACCESS_DENIED",
            "선택한 입찰 기준자료에 접근할 수 없습니다."
    ),
    BIDDING_NOTICE_ATTACHMENT_NOT_FOUND(
            "BIDDING_NOTICE_ATTACHMENT_NOT_FOUND",
            "선택한 입찰 공고 첨부파일을 찾을 수 없습니다."
    ),
    BIDDING_REVIEW_DOCUMENT_NOT_READY(
            "BIDDING_REVIEW_DOCUMENT_NOT_READY",
            "선택한 입찰 기준자료의 처리가 완료되지 않았습니다."
    ),
    BIDDING_REVIEW_UNSUPPORTED_FILE(
            "BIDDING_REVIEW_UNSUPPORTED_FILE",
            "지원하지 않는 입찰 공고 첨부파일 형식입니다."
    ),
    BIDDING_REVIEW_ACCESS_DENIED(
            "BIDDING_REVIEW_ACCESS_DENIED",
            "해당 입찰 문서 검토에 접근할 수 없습니다."
    ),
    BIDDING_REVIEW_NOT_FOUND(
            "BIDDING_REVIEW_NOT_FOUND",
            "입찰 문서 검토를 찾을 수 없습니다."
    ),
    BIDDING_REVIEW_NOT_ABANDONABLE(
            "BIDDING_REVIEW_NOT_ABANDONABLE",
            "현재 상태에서는 입찰 문서 검토를 포기할 수 없습니다."
    ),
    BIDDING_REVIEW_JOB_NOT_FOUND(
            "BIDDING_REVIEW_JOB_NOT_FOUND",
            "현재 처리 시도와 일치하는 입찰 문서 검토 작업을 찾을 수 없습니다."
    ),
    BIDDING_INVALID_REVIEW_CALLBACK(
            "BIDDING_INVALID_REVIEW_CALLBACK",
            "입찰 문서 검토 callback 요청값이 올바르지 않습니다."
    );

    private final String code;
    private final String message;
}