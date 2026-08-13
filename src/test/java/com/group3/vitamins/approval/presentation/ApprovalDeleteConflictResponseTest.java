package com.group3.vitamins.approval.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.presentation.api.common.ApiErrorResponse;
import com.group3.vitamins.global.presentation.api.common.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEL-016 — 프론트가 실제로 받는 응답을 고정한다. 판정 로직 테스트는 예외가 던져지는 것까지만 보는데,
 * 프론트가 분기하는 것은 {@code code} 문자열이고 화면에 뜨는 것은 {@code message} 다.
 *
 * <p>⚠️ 문자열 비교로 둔다. 필드명이나 문구가 바뀌면 프론트 연동이 조용히 깨지는데,
 * 느슨하게 검증하면 그 변경이 테스트를 통과해 버린다 (`../API.md` §3-1).
 */
class ApprovalDeleteConflictResponseTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 상태별 문구가 <b>같은 코드</b>로 나가는지 본다 — 프론트는 코드로 분기하고 문구는 그대로 띄운다. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "기술 제안서 결재가 진행 중입니다. 삭제하면 결재가 취소됩니다.",
            "기술 제안서 결재는 반려된 상태입니다. 삭제하면 재상신할 수 없습니다.",
            "기술 제안서 결재는 완료된 상태입니다. 삭제하면 승인 이력을 다시 볼 수 없습니다."
    })
    @DisplayName("확인이 필요한 블록 삭제는 409 + 명세 형식 본문으로 나간다")
    void serializesConfirmRequiredAsSpecifiedBody(String message) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/blocks/10");

        ResponseEntity<ApiErrorResponse> response = handler.handleDomainException(
                new ConflictException(ApprovalErrorCode.APPROVAL_DELETE_CONFIRM_REQUIRED, message), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(objectMapper.writeValueAsString(response.getBody())).isEqualTo(
                "{\"httpStatus\":409,"
                        + "\"message\":\"" + message + "\","
                        + "\"code\":\"APPROVAL_DELETE_CONFIRM_REQUIRED\"}");
    }

    /**
     * 2026-08-12 결정 — 폴백 문구도 스텝 삭제를 권하지 않는지 본다. 스텝 삭제는 블록·이슈 전부를
     * 되돌릴 수 없이 날리고, 요구 권한도 프로젝트 EDITOR 로 달라 막다른 안내가 된다.
     *
     * <p>상태별 실제 문구는 {@code ApprovalHandlerServiceDeleteGateTest} 가 검증한다.
     */
    @Test
    @DisplayName("enum 폴백 문구도 파괴적 우회로(스텝 삭제)를 권하지 않는다")
    void doesNotAdvertiseStepDeletionWorkaround() {
        assertThat(ApprovalErrorCode.APPROVAL_DELETE_CONFIRM_REQUIRED.getMessage())
                .doesNotContain("스텝")
                .doesNotContain("결재자");
    }

    /**
     * 코드명이 「금지」가 아니라 「확인 요구」를 뜻하는지 고정한다. 폐기된 {@code APPROVAL_IN_PROGRESS} 나
     * 차단 시절의 {@code APPROVAL_ALREADY_SUBMITTED} 로 되돌아가면 프론트가 재요청 가능한 상황을
     * 실패로 끝낸다.
     */
    @Test
    @DisplayName("코드 문자열이 확인 요구 의미를 유지한다")
    void keepsConfirmRequiredCodeString() {
        assertThat(ApprovalErrorCode.APPROVAL_DELETE_CONFIRM_REQUIRED.getCode())
                .isEqualTo("APPROVAL_DELETE_CONFIRM_REQUIRED")
                .isNotEqualTo("APPROVAL_IN_PROGRESS")
                .isNotEqualTo("APPROVAL_ALREADY_SUBMITTED");
    }
}
