package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.command.RemoveMemberCommand;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.port.StagePermissionDefaultCleanupPort;
import com.group3.vitamins.project.application.port.StepPermissionCleanupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.model.ProjectMember;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * 참여자 제거의 <b>정리 훅</b>만 다룬다 (STG-004). 추가·권한변경은 범위 밖이다.
 *
 * <p>정리 포트 호출을 지워도 컴파일·다른 테스트가 전부 통과하기 때문에 여기서 못박는다 —
 * 2026-08-06 스텝 권한 누수, 2026-08-09 스테이지 기본값 누수가 같은 계열이다.
 */
@ExtendWith(MockitoExtension.class)
class ProjectMemberCommandServiceTest {

    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private StepPermissionCleanupPort stepPermissionCleanupPort;
    @Mock private StagePermissionDefaultCleanupPort stagePermissionDefaultCleanupPort;

    @InjectMocks private ProjectMemberCommandService projectMemberCommandService;

    private static final Long PROJECT_ID = 3L;
    private static final Long MEMBER_ID = 55L;
    private static final String REQUESTER = "E2024001";
    private static final String TARGET = "E2024007";

    @Test
    @DisplayName("제거하면 스텝 오버라이드와 스테이지 기본값을 둘 다 지운다")
    void 정리_두_곳() {
        givenMember(TARGET);

        projectMemberCommandService.removeMember(command());

        Mockito.verify(stepPermissionCleanupPort).deleteByProjectIdAndUserId(PROJECT_ID, TARGET);
        Mockito.verify(stagePermissionDefaultCleanupPort)
                .deleteByProjectIdAndUserId(PROJECT_ID, TARGET);
    }

    @Test
    @DisplayName("정리가 참여자 행 삭제보다 먼저다")
    void 정리_후_삭제() {
        givenMember(TARGET);

        projectMemberCommandService.removeMember(command());

        InOrder inOrder = Mockito.inOrder(
                stepPermissionCleanupPort, stagePermissionDefaultCleanupPort,
                projectMemberRepository);
        inOrder.verify(stepPermissionCleanupPort).deleteByProjectIdAndUserId(PROJECT_ID, TARGET);
        inOrder.verify(stagePermissionDefaultCleanupPort)
                .deleteByProjectIdAndUserId(PROJECT_ID, TARGET);
        inOrder.verify(projectMemberRepository).deleteById(MEMBER_ID);
    }

    @Test
    @DisplayName("자기 자신은 못 지운다 — 403 이고 정리도 일어나지 않는다")
    void 자기자신() {
        givenMember(REQUESTER);

        assertThatThrownBy(() -> projectMemberCommandService.removeMember(command()))
                .isInstanceOf(ForbiddenException.class);

        verifyNoCleanup();
        Mockito.verify(projectMemberRepository, Mockito.never()).deleteById(Mockito.anyLong());
    }

    @Test
    @DisplayName("다른 프로젝트의 참여자 행이면 404 이고 정리도 일어나지 않는다")
    void 남의_프로젝트_행() {
        given(projectMemberRepository.findById(MEMBER_ID))
                .willReturn(Optional.of(member(99L, TARGET)));

        assertThatThrownBy(() -> projectMemberCommandService.removeMember(command()))
                .isInstanceOf(NotFoundException.class);

        verifyNoCleanup();
    }

    @Test
    @DisplayName("편집 권한이 없으면 조회도 정리도 하지 않는다")
    void 권한_거부() {
        Mockito.doThrow(new ForbiddenException(ProjectErrorCode.PROJECT_EDIT_DENIED))
                .when(projectAccessUseCase).requireEditable(PROJECT_ID, REQUESTER, "USER");

        assertThatThrownBy(() -> projectMemberCommandService.removeMember(command()))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verifyNoInteractions(projectMemberRepository);
        verifyNoCleanup();
    }

    private RemoveMemberCommand command() {
        return new RemoveMemberCommand(PROJECT_ID, MEMBER_ID, REQUESTER, "USER");
    }

    private void givenMember(String userId) {
        given(projectMemberRepository.findById(MEMBER_ID))
                .willReturn(Optional.of(member(PROJECT_ID, userId)));
    }

    private ProjectMember member(Long projectId, String userId) {
        return ProjectMember.restore(MEMBER_ID, projectId, userId, MemberPermission.EDITOR,
                LocalDateTime.of(2026, 8, 1, 9, 0));
    }

    private void verifyNoCleanup() {
        Mockito.verifyNoInteractions(stepPermissionCleanupPort);
        Mockito.verifyNoInteractions(stagePermissionDefaultCleanupPort);
    }
}
