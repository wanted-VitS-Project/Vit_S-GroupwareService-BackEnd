package com.group3.vitamins.vitamate.filecleanup.application.model;

public class VitamateCleanupJob {

    private VitamateCleanupJob() {
    }

    public enum Status {
        WAITING,
        PUBLISHED,
        PROCESSING,
        RETRY_WAIT,
        COMPLETED,
        DEAD_LETTER
    }
}