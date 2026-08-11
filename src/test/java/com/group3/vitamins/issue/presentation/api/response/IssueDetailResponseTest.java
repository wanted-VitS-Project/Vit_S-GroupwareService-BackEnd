package com.group3.vitamins.issue.presentation.api.response;

import com.group3.vitamins.issue.application.result.IssueResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IssueDetailResponse")
class IssueDetailResponseTest {

    @Test
    @DisplayName("퇴사한 담당자의 퇴사일을 응답에 포함한다")
    void from_includesAssigneeResignedAt() {
        IssueResult result = new IssueResult(
                101L,
                1,
                10L,
                "제안서 1차 초안 작성",
                null,
                "TODO",
                "HIGH",
                LocalDateTime.of(2026, 8, 5, 0, 0),
                null,
                List.of(new IssueResult.AssigneeResult(
                        "EMP001", "김용준", LocalDate.of(2026, 8, 1))),
                List.of()
        );

        IssueDetailResponse response = IssueDetailResponse.from(result);

        assertThat(response.assignees()).containsExactly(
                new IssueDetailResponse.AssigneeResponse(
                        "EMP001", "김용준", LocalDate.of(2026, 8, 1)));
    }
}
