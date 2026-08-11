package com.group3.vitamins.issue.presentation.api.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IssueStatusChangeRequest")
class IssueStatusChangeRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("상태와 1 이상의 version이 있으면 유효하다")
    void validatesRequiredFields() {
        assertThat(validator.validate(new IssueStatusChangeRequest("DONE", 7))).isEmpty();
    }

    @Test
    @DisplayName("상태 누락과 공백은 ISS_STATUS_REQUIRED 검증 메시지를 낸다")
    void rejectsMissingOrBlankStatus() {
        Arrays.asList(null, "", " ").forEach(status ->
                assertThat(validator.validate(new IssueStatusChangeRequest(status, 1)))
                        .extracting(violation -> violation.getMessage())
                        .contains("ISS_STATUS_REQUIRED|상태가 전달되지 않았습니다."));
    }

    @Test
    @DisplayName("version 누락·0·음수는 ISS_INVALID_REQUEST 검증 메시지를 낸다")
    void rejectsMissingOrNonPositiveVersion() {
        Arrays.asList(null, 0, -1).forEach(version ->
                assertThat(validator.validate(new IssueStatusChangeRequest("DONE", version)))
                        .extracting(violation -> violation.getMessage())
                        .containsAnyOf("ISS_INVALID_REQUEST|버전은 필수입니다.",
                                "ISS_INVALID_REQUEST|버전은 1 이상이어야 합니다."));
    }
}
