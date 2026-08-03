package com.group3.vitamins.auth.presentation.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(

        @Schema(description = "사번. 로그인 아이디로 사용한다", example = "EMP001")
        @NotBlank(message = "사번을 입력해 주세요.")
        String userId,

        @Schema(description = "비밀번호")
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
