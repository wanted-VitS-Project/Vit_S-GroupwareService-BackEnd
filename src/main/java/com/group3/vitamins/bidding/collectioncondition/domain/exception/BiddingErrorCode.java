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

    BIDDING_INVALID_COLLECTION_SCHEDULE(
            "BIDDING_INVALID_COLLECTION_SCHEDULE",
            "자동 수집 일정이 올바르지 않습니다."
    ),

    BIDDING_INVALID_NOTICE_QUERY(
            "BIDDING_INVALID_NOTICE_QUERY",
            "입찰 공고 조회 조건이 올바르지 않습니다."
    ),

    BIDDING_NOTICE_NOT_FOUND(
            "BIDDING_NOTICE_NOT_FOUND",
            "입찰 공고를 찾을 수 없습니다."
    ),

    BIDDING_INVALID_MANUAL_NOTICE(
            "BIDDING_INVALID_MANUAL_NOTICE",
            "직접 등록 입찰 공고의 입력값이 올바르지 않습니다."
    ),

    BIDDING_MANUAL_NOTICE_DUPLICATED(
            "BIDDING_MANUAL_NOTICE_DUPLICATED",
            "현재 회사에 같은 직접 등록 입찰 공고가 존재합니다."
    ),

    BIDDING_NOTICE_EDIT_NOT_ALLOWED(
            "BIDDING_NOTICE_EDIT_NOT_ALLOWED",
            "수정할 수 없는 입찰 공고입니다."
    ),

    BIDDING_INVALID_DISMISS_REASON(
            "BIDDING_INVALID_DISMISS_REASON",
            "입찰 공고 제외 사유가 올바르지 않습니다."
    ),

    BIDDING_NOTICE_ALREADY_DISMISSED(
            "BIDDING_NOTICE_ALREADY_DISMISSED",
            "이미 제외된 입찰 공고입니다."
    ),

    BIDDING_NOTICE_NOT_DISMISSED(
            "BIDDING_NOTICE_NOT_DISMISSED",
            "제외 상태가 아닌 입찰 공고입니다."
    ),

    BIDDING_INVALID_SUMMARY_REQUEST(
            "BIDDING_INVALID_SUMMARY_REQUEST",
            "입찰 공고 AI 요약 요청이 올바르지 않습니다."
    ),

    BIDDING_SUMMARY_ALREADY_PROCESSING(
            "BIDDING_SUMMARY_ALREADY_PROCESSING",
            "현재 사용자가 요청한 AI 요약이 처리 중입니다."
    ),
    BIDDING_INVALID_SUMMARY_JOB_REQUEST(
            "BIDDING_INVALID_SUMMARY_JOB_REQUEST",
            "입찰 공고 AI 요약 작업 조회 요청이 올바르지 않습니다."
    ),

    BIDDING_SUMMARY_JOB_NOT_FOUND(
            "BIDDING_SUMMARY_JOB_NOT_FOUND",
            "현재 처리 가능한 입찰 공고 AI 요약 작업을 찾을 수 없습니다."
    ),

    BIDDING_INVALID_SUMMARY_CALLBACK(
            "BIDDING_INVALID_SUMMARY_CALLBACK",
            "입찰 공고 AI 요약 callback 요청이 올바르지 않습니다."
    ),

    BIDDING_SUMMARY_NOT_FOUND(
            "BIDDING_SUMMARY_NOT_FOUND",
            "입찰 공고 AI 요약을 찾을 수 없습니다."
    ),

    BIDDING_INVALID_SUMMARY_UPDATE(
            "BIDDING_INVALID_SUMMARY_UPDATE",
            "입찰 공고 AI 요약 수정값이 올바르지 않습니다."
    ),

    BIDDING_SUMMARY_NOT_EDITABLE(
            "BIDDING_SUMMARY_NOT_EDITABLE",
            "수정할 수 없는 입찰 공고 AI 요약입니다."
    ),

    BIDDING_SUMMARY_NOT_COMPLETED(
            "BIDDING_SUMMARY_NOT_COMPLETED",
            "완료되지 않은 입찰 공고 AI 요약은 확정할 수 없습니다."
    ),

    BIDDING_SUMMARY_ALREADY_CONFIRMED(
            "BIDDING_SUMMARY_ALREADY_CONFIRMED",
            "이미 확정된 입찰 공고 AI 요약입니다."
    ),

    BIDDING_SUMMARY_NOT_CONFIRMED(
            "BIDDING_SUMMARY_NOT_CONFIRMED",
            "확정되지 않은 입찰 공고 AI 요약은 프로젝트 전환에 사용할 수 없습니다."
    ),

    BIDDING_SUMMARY_ALREADY_LINKED(
            "BIDDING_SUMMARY_ALREADY_LINKED",
            "이미 다른 프로젝트에 연결된 입찰 공고 AI 요약입니다."
    );

    private final String code;
    private final String message;
}
