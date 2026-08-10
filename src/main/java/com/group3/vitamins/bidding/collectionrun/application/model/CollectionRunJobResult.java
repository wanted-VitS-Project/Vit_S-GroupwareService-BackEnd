package com.group3.vitamins.bidding.collectionrun.application.model;

public record CollectionRunJobResult(
        Outcome outcome,
        CollectionRunFailureType failureType,
        CollectionRequestCombination retryTarget
) {
    public enum Outcome {
        SUCCESS,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }

    public static CollectionRunJobResult success() {
        return new CollectionRunJobResult(Outcome.SUCCESS, null, null);
    }

    public static CollectionRunJobResult retryableFailure(
            CollectionRunFailureType failureType,
            CollectionRequestCombination retryTarget
    ) {
        return new CollectionRunJobResult(Outcome.RETRYABLE_FAILURE, failureType, retryTarget);
    }

    public static CollectionRunJobResult permanentFailure(CollectionRunFailureType failureType) {
        return new CollectionRunJobResult(Outcome.PERMANENT_FAILURE, failureType, null);
    }
}
