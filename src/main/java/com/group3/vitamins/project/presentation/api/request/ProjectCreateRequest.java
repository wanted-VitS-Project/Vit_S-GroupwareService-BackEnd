package com.group3.vitamins.project.presentation.api.request;

import com.group3.vitamins.project.application.command.CreateProjectCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "프로젝트 생성 요청")
public record ProjectCreateRequest(

        @Schema(description = "연결할 공고 ID. 생략하면 공고 없이 생성", example = "45")
        Long bidNoticeId,

        @NotBlank(message = "PROJECT_NAME_REQUIRED|과업명을 입력해 주세요.")
        @Size(max = 300, message = "PROJECT_NAME_TOO_LONG|과업명은 300자를 넘을 수 없습니다.")
        @Schema(description = "과업명 (최대 300자)", example = "OO시 상수도 관리 용역")
        String name,

        @Schema(description = "설명", example = "상수도 관리 시스템 고도화 용역")
        String description,

        @Schema(description = "발주처 (최대 200자)", example = "OO시청")
        String clientName,

        @Schema(description = "시작일", example = "2026-08-01")
        LocalDate startedOn,

        @Schema(description = "종료일", example = "2026-12-31")
        LocalDate endedOn,

        @PositiveOrZero(message = "CONTRACT_AMOUNT_INVALID|계약금액은 0보다 작을 수 없습니다.")
        @Schema(description = "계약금액", example = "120000000")
        BigDecimal contractAmount,

        @Schema(description = "사업 카테고리 ID 목록 (복수 선택 가능)", example = "[1, 4]")
        List<Long> businessCategoryIds
) {

    /** 요청을 서비스 커맨드로 옮긴다. requesterUserId 는 세션에서 가져온 값이라 요청 바디에 없다. */
    public CreateProjectCommand toCommand(String requesterUserId) {
        return new CreateProjectCommand(bidNoticeId, name, description, clientName,
                startedOn, endedOn, contractAmount, businessCategoryIds, requesterUserId);
    }
}
