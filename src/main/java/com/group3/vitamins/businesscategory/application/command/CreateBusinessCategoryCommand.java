
package com.group3.vitamins.businesscategory.application.command;

public record CreateBusinessCategoryCommand(
        String name,
        String code,
        String description,
        String role
) {
}