package com.group3.vitamins.issue.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.application.query.IssueListQuery;
import com.group3.vitamins.issue.application.result.IssueResult;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Issue Query Service")
class IssueQueryServiceTest {

    private final IssueStepAccessPort issueStepAccessPort = mock(IssueStepAccessPort.class);
    private final IssueQueryPort issueQueryPort = mock(IssueQueryPort.class);
    private final IssueQueryService service = new IssueQueryService(issueStepAccessPort, issueQueryPort);

    @Test
    @DisplayName("Step 접근 권한 확인 후 이슈 목록과 관계 목록을 조립한다")
    void getIssues_withRelations() {
        IssueListQuery query = new IssueListQuery(10L, null, "EMP001", "MEMBER");
        IssueResult issue = new IssueResult(
                101L,
                10L,
                "경쟁사 제안서 벤치마킹",
                null,
                "TODO",
                "HIGH",
                LocalDateTime.of(2026, 7, 25, 0, 0),
                null,
                List.of(),
                List.of()
        );

        when(issueQueryPort.findIssues(10L, null)).thenReturn(List.of(issue));
        when(issueQueryPort.findAssignees(List.of(101L))).thenReturn(List.of(
                new IssueQueryPort.AssigneeResult(101L, "EMP001", "김용준")
        ));
        when(issueQueryPort.findRelatedBlocks(List.of(101L))).thenReturn(List.of(
                new IssueQueryPort.RelatedBlockResult(101L, 15L, "제안서 작성 체크리스트", "CHECKLIST")
        ));

        var result = service.getIssues(query);

        assertThat(result.issues()).hasSize(1);
        IssueResult actual = result.issues().get(0);
        assertThat(actual.assignees()).containsExactly(
                new IssueResult.AssigneeResult("EMP001", "김용준")
        );
        assertThat(actual.relatedBlocks()).containsExactly(
                new IssueResult.BlockResult(15L, "제안서 작성 체크리스트", "CHECKLIST")
        );
        verify(issueStepAccessPort).requireAccess(10L, "EMP001", "MEMBER");
    }

    @Test
    @DisplayName("조회 결과가 없으면 관계 조회 없이 빈 목록을 반환한다")
    void getIssues_empty() {
        IssueListQuery query = new IssueListQuery(10L, null, "EMP001", "MEMBER");
        when(issueQueryPort.findIssues(10L, null)).thenReturn(List.of());

        var result = service.getIssues(query);

        assertThat(result.issues()).isEmpty();
        verify(issueQueryPort).findIssues(10L, null);
        verifyNoMoreInteractions(issueQueryPort);
    }

    @Test
    @DisplayName("blockId가 요청 Step에 속하지 않으면 예외를 던진다")
    void getIssues_blockStepMismatch() {
        IssueListQuery query = new IssueListQuery(10L, 15L, "EMP001", "MEMBER");
        when(issueQueryPort.findBlockStep(15L))
                .thenReturn(Optional.of(new IssueQueryPort.BlockStepResult(15L, 11L)));

        assertThatThrownBy(() -> service.getIssues(query))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(IssueErrorCode.ISS_BLOCK_STEP_MISMATCH));
    }
}
