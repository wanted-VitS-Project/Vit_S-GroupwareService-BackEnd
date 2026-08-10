package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception;

// 나라장터 API 호출 또는 응답 변환 실패를 나타냅니다.
public class NaraBidNoticeClientException extends RuntimeException {

    private final boolean retryable;

    public NaraBidNoticeClientException(String message) {
        this(message, false, null);
    }

    public NaraBidNoticeClientException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
