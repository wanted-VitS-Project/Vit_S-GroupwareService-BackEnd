package com.group3.vitamins.vitamate.presentation.internal.dto.request;

import com.group3.vitamins.vitamate.analysis.presentation.internal.dto.request.VitamateAnalysisCallbackRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VitamateAnalysisCallbackRequest validation")
class VitamateAnalysisCallbackRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("citations 안에 null 항목이 있으면 요청 DTO 검증에 실패한다")
    void rejectsNullCitationElement() {
        VitamateAnalysisCallbackRequest request = new VitamateAnalysisCallbackRequest(
                "attempt-1",
                "COMPLETED",
                "analysis result",
                Collections.singletonList(null),
                null
        );

        assertThat(validator.validate(request))
                .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                        .contains("citations"));
    }
}
