package com.group3.vitamins.issue.presentation.api.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IssueUpdateRequest")
class IssueUpdateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("미전달 필드와 명시적 null 필드를 구분한다")
    void from_distinguishesAbsentAndNull() throws Exception {
        IssueUpdateRequest request = IssueUpdateRequest.from(objectMapper.readTree("""
                {
                  "content": null,
                  "dueDate": null,
                  "assigneeIds": []
                }
                """));

        assertThat(request.title().present()).isFalse();
        assertThat(request.content().present()).isTrue();
        assertThat(request.content().value()).isNull();
        assertThat(request.dueDate().present()).isTrue();
        assertThat(request.dueDate().value()).isNull();
        assertThat(request.assigneeIds().present()).isTrue();
        assertThat(request.assigneeIds().value()).isEmpty();
        assertThat(request.blockIds().present()).isFalse();
    }

    @Test
    @DisplayName("LocalDate와 관계 목록을 파싱한다")
    void from_parsesDateAndLists() throws Exception {
        IssueUpdateRequest request = IssueUpdateRequest.from(objectMapper.readTree("""
                {
                  "dueDate": "2026-08-07",
                  "assigneeIds": ["EMP003", "EMP005"],
                  "blockIds": [15, 18]
                }
                """));

        assertThat(request.dueDate().value()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(request.assigneeIds().value()).containsExactly("EMP003", "EMP005");
        assertThat(request.blockIds().value()).containsExactly(15L, 18L);
    }

    @Test
    @DisplayName("마감일 형식이 LocalDate가 아니면 ISS_INVALID_REQUEST를 던진다")
    void from_invalidDate() throws Exception {
        var body = objectMapper.readTree("""
                {
                  "dueDate": "2026-08-07T18:00:00"
                }
                """);

        assertThatThrownBy(() -> IssueUpdateRequest.from(body))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_INVALID_REQUEST));
    }

    @Test
    @DisplayName("관계 목록 null 전달은 present null로 보존한다")
    void from_nullRelationList() throws Exception {
        IssueUpdateRequest request = IssueUpdateRequest.from(objectMapper.readTree("""
                {
                  "assigneeIds": null,
                  "blockIds": null
                }
                """));

        assertThat(request.assigneeIds().present()).isTrue();
        assertThat(request.assigneeIds().value()).isNull();
        assertThat(request.blockIds().present()).isTrue();
        assertThat(request.blockIds().value()).isNull();
    }
}
