package com.group3.vitamins.project.block.application.command;

/** rowIndex·sortOrder·colSpan 은 미지정을 구분해야 해서 Integer 다. */
public record CreateBlockCommand(
        Long stepId,
        String type,
        String title,
        String owner,
        Integer rowIndex,
        Integer sortOrder,
        Integer colSpan,
        String requesterUserId,
        String role
) {
}