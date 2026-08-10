package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception;

// 나라장터 API 호출 또는 응답 변환 실패를 나타냅니다.
public class NaraBidNoticeClientException extends RuntimeException {

    public NaraBidNoticeClientException(String message) {
        super(message);
    }

    public NaraBidNoticeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}