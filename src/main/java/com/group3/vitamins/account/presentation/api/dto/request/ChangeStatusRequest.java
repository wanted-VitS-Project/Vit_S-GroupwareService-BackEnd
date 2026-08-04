package com.group3.vitamins.account.presentation.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 계정 상태 변경 요청 (`.ai/api/account.md` §2).
 *
 * <p>값 검증은 서비스에서 {@code ACC_INVALID_STATUS} 로 한다 (요청 DTO 에 Bean Validation 을 걸지 않는다).
 */
public record ChangeStatusRequest(
        @Schema(description = "변경할 계정 상태", allowableValues = {"ACTIVE", "INACTIVE"}, example = "INACTIVE")
        String status
) {
}
