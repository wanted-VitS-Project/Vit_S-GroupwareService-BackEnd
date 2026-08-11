package com.group3.vitamins.activitylog.infrastructure.persistence;

import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ActivityLogActionConverter implements AttributeConverter<ActivityLogAction, String> {

    @Override
    public String convertToDatabaseColumn(ActivityLogAction attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case CREATE -> "create";
            case MODIFY -> "modify";
            case DELETE -> "delete";
            case RESTORE -> "restore";
            case PURGE -> "purge";
        };
    }

    @Override
    public ActivityLogAction convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "create" -> ActivityLogAction.CREATE;
            case "modify" -> ActivityLogAction.MODIFY;
            case "delete" -> ActivityLogAction.DELETE;
            case "restore" -> ActivityLogAction.RESTORE;
            case "purge" -> ActivityLogAction.PURGE;
            default -> throw new IllegalArgumentException("Unknown activity log action: " + dbData);
        };
    }
}
