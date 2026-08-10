package com.group3.vitamins.bidding.collectionrun.application.model;

public record CollectionRunJobResult(
        Outcome outcome,
        CollectionRunFailureType failureType
) {
    public enum Outcome {
        SUCCESS,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public static CollectionRunJobResult success() {
        return new CollectionRunJobResult(Outcome.SUCCESS, null);
    }

    public static CollectionRunJobResult retryableFailure(CollectionRunFailureType failureType) {
        return new CollectionRunJobResult(Outcome.RETRYABLE_FAILURE, failureType);
    }

    public static CollectionRunJobResult permanentFailure(CollectionRunFailureType failureType) {
        return new CollectionRunJobResult(Outcome.PERMANENT_FAILURE, failureType);
    }
}
