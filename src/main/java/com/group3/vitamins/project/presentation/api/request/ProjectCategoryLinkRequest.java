package com.group3.vitamins.project.presentation.api.request;

import com.group3.vitamins.project.application.command.LinkBusinessCategoriesCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** 사업 카테고리 연결 요청 (PRJ-007). 이미 연결된 카테고리가 섞이면 409 다. */
@Schema(description = "사업 카테고리 연결 요청")
public record ProjectCategoryLinkRequest(

        @NotEmpty(message = "CATEGORY_IDS_REQUIRED|연결할 사업 카테고리를 선택해 주세요.")
        @Schema(description = "연결할 사업 카테고리 ID 목록", example = "[1, 4]")
        List<Long> categoryIds
) {

    public LinkBusinessCategoriesCommand toCommand(Long projectId, String requesterUserId,
                                                   String role) {
        return new LinkBusinessCategoriesCommand(projectId, categoryIds, requesterUserId, role);
    }
}
