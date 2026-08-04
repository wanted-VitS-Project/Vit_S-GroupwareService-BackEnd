package com.group3.vitamins.issue.domain;

public enum IssueStatus {
    TO_DO,
    IN_PROGRESS,
    DONE;

    public String toApiValue() {
        return this == TO_DO ? "TODO" : name();
    }

    public static IssueStatus fromApiValue(String value) {
        if ("TODO".equals(value)) {
            return TO_DO;
        }
        return IssueStatus.valueOf(value);
    }
}
