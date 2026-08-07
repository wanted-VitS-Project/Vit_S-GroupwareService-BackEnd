package com.group3.vitamins.issue.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.application.query.IssueCalendarQuery;
import com.group3.vitamins.issue.application.query.IssueDetailQuery;
import com.group3.vitamins.issue.application.query.IssueListQuery;
import com.group3.vitamins.issue.application.result.IssueCalendarResult;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    @DisplayName("이슈를 찾은 뒤 Step 열람 권한을 확인하고 담당자·관련 Block과 함께 상세를 반환한다")
    void getIssue_success() {
        IssueDetailQuery query = new IssueDetailQuery(101L, "EMP001", "MEMBER");
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

        when(issueQueryPort.findIssue(101L)).thenReturn(Optional.of(issue));
        when(issueQueryPort.findAssignees(List.of(101L))).thenReturn(List.of(
                new IssueQueryPort.AssigneeResult(101L, "EMP001", "김용준")
        ));
        when(issueQueryPort.findRelatedBlocks(List.of(101L))).thenReturn(List.of(
                new IssueQueryPort.RelatedBlockResult(101L, 15L, "제안서 작성 체크리스트", "CHECKLIST")
        ));

        IssueResult result = service.getIssue(query);

        assertThat(result.issueId()).isEqualTo(101L);
        assertThat(result.stepId()).isEqualTo(10L);
        assertThat(result.assignees()).containsExactly(
                new IssueResult.AssigneeResult("EMP001", "김용준")
        );
        assertThat(result.relatedBlocks()).containsExactly(
                new IssueResult.BlockResult(15L, "제안서 작성 체크리스트", "CHECKLIST")
        );
        verify(issueStepAccessPort).requireIssueAccess(10L, "EMP001", "MEMBER");
    }

    @Test
    @DisplayName("이슈가 없거나 이미 삭제됐으면 권한 확인 없이 404를 던진다")
    void getIssue_notFound() {
        IssueDetailQuery query = new IssueDetailQuery(101L, "EMP001", "MEMBER");
        when(issueQueryPort.findIssue(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getIssue(query))
                .isInstanceOfSatisfying(NotFoundException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_NOT_FOUND));

        verifyNoInteractions(issueStepAccessPort);
        verify(issueQueryPort, never()).findAssignees(org.mockito.ArgumentMatchers.anyList());
        verify(issueQueryPort, never()).findRelatedBlocks(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("소속 Step 열람 권한이 없으면 예외를 전파하고 관계를 조회하지 않는다")
    void getIssue_forbidden() {
        IssueDetailQuery query = new IssueDetailQuery(101L, "EMP001", "MEMBER");
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

        when(issueQueryPort.findIssue(101L)).thenReturn(Optional.of(issue));
        when(issueStepAccessPort.requireIssueAccess(10L, "EMP001", "MEMBER"))
                .thenThrow(new ForbiddenException(IssueErrorCode.ISS_ACCESS_PERMISSION_REQUIRED));

        assertThatThrownBy(() -> service.getIssue(query))
                .isInstanceOfSatisfying(ForbiddenException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                IssueErrorCode.ISS_ACCESS_PERMISSION_REQUIRED));

        verify(issueQueryPort, never()).findAssignees(org.mockito.ArgumentMatchers.anyList());
        verify(issueQueryPort, never()).findRelatedBlocks(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("포트가 돌려준 본인 담당 이슈를 캘린더 결과로 그대로 매핑한다")
    void getMyCalendarIssues_success() {
        IssueCalendarQuery query = new IssueCalendarQuery("EMP001");
        IssueQueryPort.CalendarIssueResult row = new IssueQueryPort.CalendarIssueResult(
                101L,
                "제안서 1차 초안 작성",
                "IN_PROGRESS",
                "HIGH",
                LocalDateTime.of(2026, 8, 11, 0, 0),
                10L,
                "입찰 진행",
                3L,
                "OO시 스마트도로 구축"
        );
        when(issueQueryPort.findMyCalendarIssues("EMP001")).thenReturn(List.of(row));

        IssueCalendarResult result = service.getMyCalendarIssues(query);

        assertThat(result.issues()).containsExactly(
                new IssueCalendarResult.CalendarIssueResult(
                        101L,
                        "제안서 1차 초안 작성",
                        "IN_PROGRESS",
                        "HIGH",
                        LocalDateTime.of(2026, 8, 11, 0, 0),
                        10L,
                        "입찰 진행",
                        3L,
                        "OO시 스마트도로 구축"
                )
        );
        verify(issueQueryPort).findMyCalendarIssues("EMP001");
        verifyNoInteractions(issueStepAccessPort);
    }

    @Test
    @DisplayName("담당 이슈가 없으면 빈 목록을 반환한다")
    void getMyCalendarIssues_empty() {
        IssueCalendarQuery query = new IssueCalendarQuery("EMP001");
        when(issueQueryPort.findMyCalendarIssues("EMP001")).thenReturn(List.of());

        IssueCalendarResult result = service.getMyCalendarIssues(query);

        assertThat(result.issues()).isEmpty();
    }
}
