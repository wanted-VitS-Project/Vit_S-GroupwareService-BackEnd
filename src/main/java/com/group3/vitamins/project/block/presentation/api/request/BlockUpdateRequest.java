package com.group3.vitamins.project.block.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 블록 제목·담당자 수정 요청. 실제 바인딩은 컨트롤러가 JsonNode 로 받는다 —
 * "생략" 과 "null 명시" 를 구분해야 해서다. 이 record 는 Swagger 스키마 노출용이다.
 */
@Schema(description = "블록 제목·담당자 수정 요청 — 보낸 필드만 바뀐다. 둘 다 없으면 400")
public record BlockUpdateRequest(

        @Schema(description = "블록 제목 (최대 200자). null 을 보내면 제목을 지운다. 생략하면 안 바뀐다",
                example = "핵심 요구사항 메모", nullable = true)
        String title,

        @Schema(description = "블록 담당자 사번. null 을 보내면 담당자를 해제한다. 생략하면 안 바뀐다",
                example = "E2024001", nullable = true)
        String owner,

        @Schema(description = "조회에서 받은 version 을 그대로 실어 보낸다. 누락하면 400",
                example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer version,

        @Schema(description = "true 면 충돌을 무시하고 덮어쓴다. 생략하면 false", example = "false")
        Boolean overwrite
) {
}