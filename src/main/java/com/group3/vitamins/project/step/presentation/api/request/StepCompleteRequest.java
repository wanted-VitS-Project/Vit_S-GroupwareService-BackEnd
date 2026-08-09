package com.group3.vitamins.project.step.presentation.api.request;

import com.group3.vitamins.project.step.application.command.CompleteStepCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 스텝 완료 요청. 미완료 이슈가 남아 있어도 완료는 진행된다(STP-005). */
@Schema(description = "스텝 완료 처리 요청")
public record StepCompleteRequest(

        @NotBlank(message = "OPEN_ISSUE_ACTION_REQUIRED|미완료 이슈 처리 방식을 선택해 주세요.")
        @Schema(description = "KEEP(그대로 두기) · CLOSE(함께 종료)", example = "KEEP",
                allowableValues = {"KEEP", "CLOSE"})
        String openIssueAction
) {

    public CompleteStepCommand toCommand(Long stepId, String requesterUserId, String role) {
        return new CompleteStepCommand(stepId, openIssueAction, requesterUserId, role);
    }
}
