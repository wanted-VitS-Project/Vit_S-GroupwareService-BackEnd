package com.group3.vitamins.bidding.collectionrun.application.command;

public record StartCollectionRunCommand(
        Long conditionId,
        String userId
) {
}
