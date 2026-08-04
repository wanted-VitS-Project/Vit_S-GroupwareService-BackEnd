package com.group3.vitamins.activitylog.contract;

public record ActivityFieldChange(
        String field,
        String beforeValue,
        String afterValue
) {
}
