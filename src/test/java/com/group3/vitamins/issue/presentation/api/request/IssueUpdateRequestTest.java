package com.group3.vitamins.issue.presentation.api.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

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
                  "version": 1,
                  "content": null,
                  "dueDate": null,
                  "assigneeIds": []
                }
                """));

        assertThat(request.title().present()).isFalse();
        assertThat(request.version()).isEqualTo(1);
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
                  "version": 3,
                  "dueDate": "2026-08-07",
                  "assigneeIds": ["EMP003", "EMP005"],
                  "blockIds": [15, 18]
                }
                """));

        assertThat(request.dueDate().value()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(request.version()).isEqualTo(3);
        assertThat(request.assigneeIds().value()).containsExactly("EMP003", "EMP005");
        assertThat(request.blockIds().value()).containsExactly(15L, 18L);
    }

    @Test
    @DisplayName("마감일 형식이 LocalDate가 아니면 ISS_INVALID_REQUEST를 던진다")
    void from_invalidDate() throws Exception {
        assertInvalid("""
                {
                  "version": 1,
                  "dueDate": "2026-08-07T18:00:00"
                }
                """);
    }

    @Test
    @DisplayName("문자열 필드에 문자열 또는 null이 아닌 값이 오면 ISS_INVALID_REQUEST를 던진다")
    void from_invalidTextFieldType() {
        List<String> invalidRequests = List.of(
                """
                {
                  "version": 1,
                  "title": ["제안서"]
                }
                """,
                """
                {
                  "version": 1,
                  "content": {
                    "text": "공고 요구사항"
                  }
                }
                """,
                """
                {
                  "version": 1,
                  "priority": true
                }
                """
        );

        invalidRequests.forEach(this::assertInvalid);
    }

    @Test
    @DisplayName("마감일에 문자열 또는 null이 아닌 값이 오면 ISS_INVALID_REQUEST를 던진다")
    void from_invalidDateFieldType() {
        List<String> invalidRequests = List.of(
                """
                {
                  "version": 1,
                  "dueDate": 20260807
                }
                """,
                """
                {
                  "version": 1,
                  "dueDate": {
                    "date": "2026-08-07"
                  }
                }
                """,
                """
                {
                  "version": 1,
                  "dueDate": ["2026-08-07"]
                }
                """
        );

        invalidRequests.forEach(this::assertInvalid);
    }

    @Test
    @DisplayName("담당자 목록에 문자열이 아닌 항목이 오면 ISS_INVALID_REQUEST를 던진다")
    void from_invalidAssigneeItemType() {
        List<String> invalidRequests = List.of(
                """
                {
                  "version": 1,
                  "assigneeIds": ["EMP003", null]
                }
                """,
                """
                {
                  "version": 1,
                  "assigneeIds": ["EMP003", {
                    "userId": "EMP005"
                  }]
                }
                """,
                """
                {
                  "version": 1,
                  "assigneeIds": ["EMP003", 5]
                }
                """
        );

        invalidRequests.forEach(this::assertInvalid);
    }

    @Test
    @DisplayName("Block 목록에 정수 Long이 아닌 항목이 오면 ISS_INVALID_REQUEST를 던진다")
    void from_invalidBlockItemType() {
        List<String> invalidRequests = List.of(
                """
                {
                  "version": 1,
                  "blockIds": [15, null]
                }
                """,
                """
                {
                  "version": 1,
                  "blockIds": [15, "18"]
                }
                """,
                """
                {
                  "version": 1,
                  "blockIds": [15, 18.5]
                }
                """
        );

        invalidRequests.forEach(this::assertInvalid);
    }

    @Test
    @DisplayName("관계 목록 필드가 배열 또는 null이 아니면 ISS_INVALID_REQUEST를 던진다")
    void from_invalidRelationFieldType() {
        List<String> invalidRequests = List.of(
                """
                {
                  "version": 1,
                  "assigneeIds": "EMP003"
                }
                """,
                """
                {
                  "version": 1,
                  "blockIds": {
                    "blockId": 15
                  }
                }
                """
        );

        invalidRequests.forEach(this::assertInvalid);
    }

    @Test
    @DisplayName("관계 목록 null 전달은 present null로 보존한다")
    void from_nullRelationList() throws Exception {
        IssueUpdateRequest request = IssueUpdateRequest.from(objectMapper.readTree("""
                {
                  "version": 1,
                  "assigneeIds": null,
                  "blockIds": null
                }
                """));

        assertThat(request.assigneeIds().present()).isTrue();
        assertThat(request.assigneeIds().value()).isNull();
        assertThat(request.blockIds().present()).isTrue();
        assertThat(request.blockIds().value()).isNull();
    }

    @Test
    @DisplayName("version은 1 이상의 정수로 반드시 전달해야 한다")
    void from_requiresPositiveIntegralVersion() {
        List<String> invalidRequests = List.of(
                """
                { "title": "제안서" }
                """,
                """
                { "version": 0, "title": "제안서" }
                """,
                """
                { "version": -1, "title": "제안서" }
                """,
                """
                { "version": 1.5, "title": "제안서" }
                """,
                """
                { "version": "1", "title": "제안서" }
                """
        );

        invalidRequests.forEach(this::assertInvalid);
    }

    private void assertInvalid(String json) {
        assertThatThrownBy(() -> IssueUpdateRequest.from(objectMapper.readTree(json)))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_INVALID_REQUEST));
    }
}
