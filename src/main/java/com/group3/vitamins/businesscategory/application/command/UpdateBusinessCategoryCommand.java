package com.group3.vitamins.businesscategory.application.command;

public record UpdateBusinessCategoryCommand(
        Long categoryId,
        boolean nameProvided,
        String name,
        boolean codeProvided,
        String code,
        boolean descriptionProvided,
        String description,
        String role
) {
}