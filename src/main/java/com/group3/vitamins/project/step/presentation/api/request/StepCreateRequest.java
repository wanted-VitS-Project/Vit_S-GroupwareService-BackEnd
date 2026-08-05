package com.group3.vitamins.project.step.presentation.api.request;

import com.group3.vitamins.project.step.application.command.CreateStepCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "스텝 생성 요청")
public record StepCreateRequest(

        @Schema(description = "스텝명 (최대 200자)", example = "제안서 작성")
        String name,

        @Schema(description = "소속 스테이지 ID. 생략하면 미소속 스텝", example = "7")
        Long stageId,

        @Schema(description = "시작일", example = "2026-08-01")
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2026-08-10")
        LocalDate endedOn,

        @Schema(description = "책임자 사번. 책임자는 작업자가 아니다", example = "E2024001")
        String ownerUserId
) {

    /** 요청을 서비스 커맨드로 옮긴다. sortOrder 는 요청에 없다 — 시스템이 max+1 로 채운다. */
    public CreateStepCommand toCommand(Long projectId, String requesterUserId, String role) {
        return new CreateStepCommand(projectId, stageId, name,
                startedOn, endedOn, ownerUserId, requesterUserId, role);
    }
}