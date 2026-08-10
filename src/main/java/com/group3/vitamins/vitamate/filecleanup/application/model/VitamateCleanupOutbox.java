package com.group3.vitamins.vitamate.filecleanup.application.model;

public class VitamateCleanupOutbox {

    private VitamateCleanupOutbox() {
    }

    public enum PublishStatus {
        PENDING,
        PUBLISHED
    }
}