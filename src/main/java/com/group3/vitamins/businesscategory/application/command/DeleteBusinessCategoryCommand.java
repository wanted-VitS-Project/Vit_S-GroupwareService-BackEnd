package com.group3.vitamins.businesscategory.application.command;

public record DeleteBusinessCategoryCommand(
        Long categoryId,
        String role
) {
}