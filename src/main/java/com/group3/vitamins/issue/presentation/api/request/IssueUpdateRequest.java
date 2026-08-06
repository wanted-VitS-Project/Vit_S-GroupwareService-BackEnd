package com.group3.vitamins.issue.presentation.api.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.command.PatchField;
import com.group3.vitamins.issue.application.command.UpdateIssueCommand;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "이슈 부분 수정 요청")
public record IssueUpdateRequest(

        @Schema(description = "제목. 전달 시 빈 값 불가, 최대 200자", example = "제안서 최종 초안 작성")
        PatchField<String> title,

        @Schema(description = "내용. 명시적 null이면 내용 삭제")
        PatchField<String> content,

        @Schema(description = "마감일. 명시적 null이면 마감일 해제", example = "2026-08-07")
        PatchField<LocalDate> dueDate,

        @Schema(description = "LOW · MEDIUM · HIGH", example = "HIGH")
        PatchField<String> priority,

        @Schema(description = "최종 담당자 전체 목록. members[].userId 사용")
        PatchField<List<String>> assigneeIds,

        @Schema(description = "최종 관련 Block 전체 목록. blocks[].blockId 사용")
        PatchField<List<Long>> blockIds
) {

    public static IssueUpdateRequest from(JsonNode body) {
        JsonNode safeBody = body == null ? com.fasterxml.jackson.databind.node.MissingNode.getInstance() : body;
        return new IssueUpdateRequest(
                textField(safeBody, "title"),
                textField(safeBody, "content"),
                dateField(safeBody, "dueDate"),
                textField(safeBody, "priority"),
                stringListField(safeBody, "assigneeIds"),
                longListField(safeBody, "blockIds")
        );
    }

    public UpdateIssueCommand toCommand(Long issueId, String requesterUserId, String role) {
        return new UpdateIssueCommand(
                issueId,
                title,
                content,
                dueDate,
                priority,
                assigneeIds,
                blockIds,
                requesterUserId,
                role
        );
    }

    private static PatchField<String> textField(JsonNode body, String name) {
        if (!body.has(name)) {
            return PatchField.absent();
        }
        JsonNode node = body.get(name);
        return PatchField.present(node == null || node.isNull() ? null : node.asText());
    }

    private static PatchField<LocalDate> dateField(JsonNode body, String name) {
        if (!body.has(name)) {
            return PatchField.absent();
        }
        JsonNode node = body.get(name);
        if (node == null || node.isNull()) {
            return PatchField.present(null);
        }
        try {
            return PatchField.present(LocalDate.parse(node.asText()));
        } catch (DateTimeParseException e) {
            throw new ValidationException(IssueErrorCode.ISS_INVALID_REQUEST);
        }
    }

    private static PatchField<List<String>> stringListField(JsonNode body, String name) {
        if (!body.has(name)) {
            return PatchField.absent();
        }
        JsonNode node = body.get(name);
        if (node == null || node.isNull() || !node.isArray()) {
            return PatchField.present(null);
        }

        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item == null || item.isNull() ? null : item.asText());
        }
        return PatchField.present(values);
    }

    private static PatchField<List<Long>> longListField(JsonNode body, String name) {
        if (!body.has(name)) {
            return PatchField.absent();
        }
        JsonNode node = body.get(name);
        if (node == null || node.isNull() || !node.isArray()) {
            return PatchField.present(null);
        }

        List<Long> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull() || !item.canConvertToLong()) {
                values.add(null);
            } else {
                values.add(item.asLong());
            }
        }
        return PatchField.present(values);
    }
}
