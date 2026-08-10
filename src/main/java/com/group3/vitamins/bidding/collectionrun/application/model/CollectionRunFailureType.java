package com.group3.vitamins.bidding.collectionrun.application.model;

// DB, Redis, 로그에 원문 오류 대신 저장할 안전한 실패 유형입니다.
public enum CollectionRunFailureType {
    CONNECTION_FAILURE,
    TIMEOUT,
    RATE_LIMITED,
    EXTERNAL_SERVER_ERROR,
    INVALID_REQUEST,
    AUTHENTICATION_FAILURE,
    RESPONSE_PARSING_FAILURE,
    MALFORMED_MESSAGE,
    UNKNOWN_PROCESSING_ERROR
}
