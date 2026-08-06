package com.group3.vitamins.file.application;

import com.group3.vitamins.file.application.command.RenameFileCommand;
import com.group3.vitamins.file.application.command.TrashFileCommand;
import com.group3.vitamins.file.application.port.ApprovalLockQueryPort;
import com.group3.vitamins.file.application.port.BlockCatalogPort;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.result.FileRenameResult;
import com.group3.vitamins.file.application.result.FileTrashResult;
import com.group3.vitamins.file.application.service.FileCommandService;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FileCommandService 문서명 수정·휴지통 이동 (#135)")
class FileCommandServiceTest {

    private static final Long FILE_ID = 31L;
    private static final Long BLOCK_ID = 12L;
    private static final Long STEP_ID = 5L;
    private static final Long PROJECT_ID = 100L;
    private static final String USER = "EMP001";
    private static final String ROLE = "MEMBER";

    private FileRepository fileRepository;
    private FileQueryPort fileQueryPort;
    private BlockCatalogPort blockCatalogPort;
    private StepAccessUseCase stepAccessUseCase;
    private ApprovalLockQueryPort approvalLockQueryPort;
    private FileCommandService service;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(FileRepository.class);
        fileQueryPort = Mockito.mock(FileQueryPort.class);
        blockCatalogPort = Mockito.mock(BlockCatalogPort.class);
        stepAccessUseCase = Mockito.mock(StepAccessUseCase.class);
        approvalLockQueryPort = Mockito.mock(ApprovalLockQueryPort.class);
        service = new FileCommandService(
                fileRepository, fileQueryPort, blockCatalogPort, stepAccessUseCase, approvalLockQueryPort);
    }

    // ---- 헬퍼 ---------------------------------------------------------------

    private File activeFile() {
        return File.restore(FILE_ID, PROJECT_ID, "제안서", USER, null);
    }

    private File trashedFile() {
        return File.restore(FILE_ID, PROJECT_ID, "제안서", USER, LocalDateTime.now());
    }

    /** 문서 → 블록 → 스텝 편집 권한(EDITOR) 경로 스텁. */
    private void stubEditable() {
        when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
        when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
        when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.EDITOR));
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Nested
    @DisplayName("§4 문서명 수정")
    class Rename {

        private RenameFileCommand cmd(String name) {
            return new RenameFileCommand(FILE_ID, name, USER, ROLE);
        }

        @Test
        @DisplayName("표시명을 바꾸고 저장한다")
        void renames() {
            File file = activeFile();
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
            stubEditable();
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FileRenameResult result = service.rename(cmd("  제안서_최종  "));

            assertThat(result.name()).isEqualTo("제안서_최종"); // 앞뒤 공백은 정리한다
            assertThat(file.getName()).isEqualTo("제안서_최종");
        }

        @Test
        @DisplayName("이름이 비었으면 FILE_INVALID_REQUEST")
        void blankName() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubEditable();

            assertThatThrownBy(() -> service.rename(cmd("   ")))
                    .satisfies(hasCode(FileErrorCode.FILE_INVALID_REQUEST));
            verify(fileRepository, never()).save(any());
        }

        @Test
        @DisplayName("255자를 넘으면 FILE_INVALID_REQUEST")
        void tooLong() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubEditable();

            assertThatThrownBy(() -> service.rename(cmd("가".repeat(256))))
                    .satisfies(hasCode(FileErrorCode.FILE_INVALID_REQUEST));
        }

        @Test
        @DisplayName("이미 휴지통이거나 없는 문서면 FILE_NOT_FOUND")
        void notFoundWhenTrashed() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));

            assertThatThrownBy(() -> service.rename(cmd("새이름")))
                    .satisfies(hasCode(FileErrorCode.FILE_NOT_FOUND));
        }

        @Test
        @DisplayName("편집 권한이 없으면 FILE_EDIT_PERMISSION_REQUIRED")
        void notEditable() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
            when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenThrow(new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));

            assertThatThrownBy(() -> service.rename(cmd("새이름")))
                    .satisfies(hasCode(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED));
        }
    }

    @Nested
    @DisplayName("§5 휴지통 이동")
    class Trash {

        private TrashFileCommand cmd() {
            return new TrashFileCommand(FILE_ID, USER, ROLE);
        }

        @Test
        @DisplayName("삭제 시각을 기록하고 저장한다")
        void movesToTrash() {
            File file = activeFile();
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
            stubEditable();
            when(approvalLockQueryPort.findInProgressApproval(FILE_ID)).thenReturn(Optional.empty());
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FileTrashResult result = service.moveToTrash(cmd());

            assertThat(result.deletedAt()).isNotNull();
            assertThat(file.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 휴지통이면 FILE_ALREADY_DELETED")
        void alreadyDeleted() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));
            stubEditable();

            assertThatThrownBy(() -> service.moveToTrash(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_ALREADY_DELETED));
            verify(fileRepository, never()).save(any());
        }

        @Test
        @DisplayName("진행 중 결재의 대상이면 FILE_APPROVAL_IN_PROGRESS")
        void approvalInProgress() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubEditable();
            when(approvalLockQueryPort.findInProgressApproval(FILE_ID))
                    .thenReturn(Optional.of(new ApprovalLockQueryPort.InProgressApproval(7L, "제안서 최종 결재")));

            assertThatThrownBy(() -> service.moveToTrash(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_APPROVAL_IN_PROGRESS))
                    .hasMessageContaining("제안서 최종 결재");
            verify(fileRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 문서면 FILE_NOT_FOUND")
        void notFound() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.moveToTrash(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_NOT_FOUND));
        }

        @Test
        @DisplayName("편집 권한이 없으면 FILE_EDIT_PERMISSION_REQUIRED")
        void notEditable() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
            when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenThrow(new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));

            assertThatThrownBy(() -> service.moveToTrash(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED));
        }
    }
}
