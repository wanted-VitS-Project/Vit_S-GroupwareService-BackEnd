package com.group3.vitamins.jobposition.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 직급 수정 요청 (Swagger 스키마 표기용).
 *
 * <p>실제 파싱은 Controller 가 raw JSON({@code JsonNode})으로 처리한다 — "필드 생략" 과 "값 전달" 을
 * 구분해야 하는데 record 역직렬화로는 둘을 가를 수 없기 때문이다(둘 다 null).
 */
@Schema(description = "직급 수정 요청 (전달한 필드만 수정)")
public record JobPositionUpdateRequest(

        @Schema(description = "직급명 (선택, 최대 30자, 중복 불가)", example = "과장")
        String name,

        @Schema(description = "정렬 순서 (선택)", example = "3")
        Integer sortOrder
) {
}
