package com.group3.vitamins.bidding.collectionrun.application.model;

public record CollectionRunJobResult(
        Outcome outcome,
        String errorType
) {
    public enum Outcome {
        SUCCESS,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public static CollectionRunJobResult success() {
        return new CollectionRunJobResult(Outcome.SUCCESS, null);
    }

    public static CollectionRunJobResult retryableFailure(String errorType) {
        return new CollectionRunJobResult(Outcome.RETRYABLE_FAILURE, errorType);
    }

    public static CollectionRunJobResult permanentFailure(String errorType) {
        return new CollectionRunJobResult(Outcome.PERMANENT_FAILURE, errorType);
    }
}
