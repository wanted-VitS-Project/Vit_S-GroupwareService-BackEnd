package com.group3.vitamins.issue.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.issue.application.command.DeleteIssueCommand;
import com.group3.vitamins.issue.application.port.IssueAssigneePort;
import com.group3.vitamins.issue.application.port.IssueBlockPort;
import com.group3.vitamins.issue.application.port.IssueStepAccessPort;
import com.group3.vitamins.issue.domain.IssuePriority;
import com.group3.vitamins.issue.domain.IssueStatus;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import com.group3.vitamins.issue.domain.model.Issue;
import com.group3.vitamins.issue.domain.repository.IssueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Issue Command Service")
class IssueCommandServiceTest {

    private final IssueRepository issueRepository = mock(IssueRepository.class);
    private final IssueStepAccessPort issueStepAccessPort = mock(IssueStepAccessPort.class);
    private final IssueAssigneePort issueAssigneePort = mock(IssueAssigneePort.class);
    private final IssueBlockPort issueBlockPort = mock(IssueBlockPort.class);
    private final IssueCommandService service = new IssueCommandService(
            issueRepository,
            issueStepAccessPort,
            issueAssigneePort,
            issueBlockPort
    );

    @Test
    @DisplayName("이슈 삭제 시 Step 편집 권한을 확인한 뒤 논리 삭제하고 관계를 제거한다")
    void deleteIssue_success() {
        Issue issue = Issue.restore(
                101L,
                10L,
                "경쟁사 제안서 벤치마킹",
                null,
                null,
                IssueStatus.TO_DO,
                IssuePriority.HIGH,
                "EMP001",
                LocalDateTime.of(2026, 8, 1, 10, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                null,
                null
        );
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.of(issue));

        service.deleteIssue(new DeleteIssueCommand(101L, "EMP002", "MEMBER"));

        assertThat(issue.getDeletedAt()).isNotNull();
        verify(issueStepAccessPort).requireEditable(10L, "EMP002", "MEMBER");

        // Hibernate 는 @Modifying(clearAutomatically = true) 벌크 삭제 시 flush 없이 컨텍스트를 비운다 —
        // save()를 관계 삭제보다 먼저 하면 아직 flush 안 된 deletedAt 이 유실된다(BlockCommandService 와 같은 함정).
        // 그래서 관계 삭제를 먼저, Issue 본체 save()를 마지막에 호출해야 한다.
        InOrder inOrder = inOrder(issueRepository);
        inOrder.verify(issueRepository).findActiveById(101L);
        inOrder.verify(issueRepository).deleteAssignees(101L);
        inOrder.verify(issueRepository).deleteBlockLinks(101L);
        inOrder.verify(issueRepository).save(issue);
    }

    @Test
    @DisplayName("이슈가 없거나 이미 삭제됐으면 권한 확인 없이 404를 던진다")
    void deleteIssue_notFound() {
        when(issueRepository.findActiveById(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteIssue(new DeleteIssueCommand(101L, "EMP002", "MEMBER")))
                .isInstanceOfSatisfying(NotFoundException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(IssueErrorCode.ISS_NOT_FOUND));

        verify(issueStepAccessPort, never()).requireEditable(10L, "EMP002", "MEMBER");
        verify(issueRepository, never()).deleteAssignees(101L);
        verify(issueRepository, never()).deleteBlockLinks(101L);
    }
}
