package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception;

// 나라장터 공고 원문을 내부 모델로 변환하지 못했음을 나타냅니다.
public class NaraBidNoticeNormalizationException extends RuntimeException {

    public NaraBidNoticeNormalizationException(String message) {
        super(message);
    }

    public NaraBidNoticeNormalizationException(String message, Throwable cause) {
        super(message, cause);
    }
}