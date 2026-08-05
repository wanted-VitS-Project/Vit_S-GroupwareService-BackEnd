package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.ErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProjectAccessService 프로젝트 접근/편집 권한 확인")
class ProjectAccessServiceTest {

    private static final Long PROJECT_ID = 12L;
    private static final String REQUESTER_ID = "E2024001";

    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectAccessService projectAccessService;

    @BeforeEach
    void setUp() {
        projectRepository = Mockito.mock(ProjectRepository.class);
        projectMemberRepository = Mockito.mock(ProjectMemberRepository.class);
        projectAccessService = new ProjectAccessService(
                projectRepository, projectMemberRepository, new ProjectAccessPolicy());
    }

    private Project existingProject() {
        return Project.restore(PROJECT_ID, null, "프로젝트", null, ProjectStatus.NOT_STARTED,
                null, null, null, null, null, null,
                REQUESTER_ID, LocalDateTime.of(2026, 8, 1, 10, 0), null);
    }

    private void givenProjectExists() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(existingProject()));
    }

    private void givenProjectMissing() {
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.empty());
    }

    private void givenPermission(MemberPermission permission) {
        when(projectMemberRepository.findPermission(PROJECT_ID, REQUESTER_ID))
                .thenReturn(Optional.ofNullable(permission));
    }

    private Consumer<Throwable> hasCode(ErrorCode expected) {
        return thrown -> {
            assertThat(thrown).isInstanceOf(DomainException.class);
            assertThat(((DomainException) thrown).getErrorCode()).isEqualTo(expected);
        };
    }

    @Nested
    @DisplayName("requireAccess — 읽기 권한 확인")
    class RequireAccess {

        @Test
        @DisplayName("프로젝트가 없으면 PROJECT_NOT_FOUND — 참여자 조회는 하지 않는다")
        void rejectsMissingProject() {
            givenProjectMissing();

            assertThatThrownBy(() -> projectAccessService.requireAccess(PROJECT_ID, REQUESTER_ID, "MEMBER"))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NOT_FOUND));
            verify(projectMemberRepository, never()).findPermission(anyLong(), any());
        }

        @Test
        @DisplayName("참여자가 아니면 PROJECT_ACCESS_DENIED")
        void rejectsNonMember() {
            givenProjectExists();
            givenPermission(null);

            assertThatThrownBy(() -> projectAccessService.requireAccess(PROJECT_ID, REQUESTER_ID, "MEMBER"))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_ACCESS_DENIED));
        }

        @Test
        @DisplayName("VIEWER 참여자면 VIEWER 권한을 그대로 돌려준다")
        void returnsViewerPermission() {
            givenProjectExists();
            givenPermission(MemberPermission.VIEWER);

            MemberPermission result = projectAccessService.requireAccess(PROJECT_ID, REQUESTER_ID, "MEMBER");

            assertThat(result).isEqualTo(MemberPermission.VIEWER);
        }

        @Test
        @DisplayName("EDITOR 참여자면 EDITOR 권한을 그대로 돌려준다")
        void returnsEditorPermission() {
            givenProjectExists();
            givenPermission(MemberPermission.EDITOR);

            MemberPermission result = projectAccessService.requireAccess(PROJECT_ID, REQUESTER_ID, "MEMBER");

            assertThat(result).isEqualTo(MemberPermission.EDITOR);
        }

        @Test
        @DisplayName("MASTER 는 참여자가 아니어도 EDITOR 로 통과한다")
        void allowsGlobalAdminWithoutMembership() {
            givenProjectExists();
            givenPermission(null);

            MemberPermission result = projectAccessService.requireAccess(PROJECT_ID, REQUESTER_ID, "MASTER");

            assertThat(result).isEqualTo(MemberPermission.EDITOR);
        }
    }

    @Nested
    @DisplayName("requireEditable — 편집 권한 확인")
    class RequireEditable {

        @Test
        @DisplayName("프로젝트가 없으면 PROJECT_NOT_FOUND")
        void rejectsMissingProject() {
            givenProjectMissing();

            assertThatThrownBy(() -> projectAccessService.requireEditable(PROJECT_ID, REQUESTER_ID, "MEMBER"))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NOT_FOUND));
        }

        @Test
        @DisplayName("참여자가 아니면 PROJECT_EDIT_DENIED")
        void rejectsNonMember() {
            givenProjectExists();
            givenPermission(null);

            assertThatThrownBy(() -> projectAccessService.requireEditable(PROJECT_ID, REQUESTER_ID, "MEMBER"))
                    .isInstanceOf(ForbiddenException.class)
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_EDIT_DENIED));
        }

        @Test
        @DisplayName("VIEWER 권한이면 PROJECT_EDIT_DENIED")
        void rejectsViewer() {
            givenProjectExists();
            givenPermission(MemberPermission.VIEWER);

            assertThatThrownBy(() -> projectAccessService.requireEditable(PROJECT_ID, REQUESTER_ID, "MEMBER"))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_EDIT_DENIED));
        }

        @Test
        @DisplayName("EDITOR 참여자면 예외 없이 통과한다")
        void allowsEditor() {
            givenProjectExists();
            givenPermission(MemberPermission.EDITOR);

            projectAccessService.requireEditable(PROJECT_ID, REQUESTER_ID, "MEMBER");
        }

        @Test
        @DisplayName("MASTER 는 참여자가 아니어도 통과한다")
        void allowsGlobalAdminWithoutMembership() {
            givenProjectExists();
            givenPermission(null);

            projectAccessService.requireEditable(PROJECT_ID, REQUESTER_ID, "ADMIN");
        }
    }
}
