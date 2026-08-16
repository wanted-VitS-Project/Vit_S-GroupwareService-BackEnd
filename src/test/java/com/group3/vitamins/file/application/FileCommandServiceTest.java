package com.group3.vitamins.file.application;

import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.file.application.command.PermanentDeleteFileCommand;
import com.group3.vitamins.file.application.command.RenameFileCommand;
import com.group3.vitamins.file.application.command.RestoreFileCommand;
import com.group3.vitamins.file.application.command.TrashFileCommand;
import com.group3.vitamins.file.application.port.ApprovalLockQueryPort;
import com.group3.vitamins.file.application.port.BlockCatalogPort;
import com.group3.vitamins.file.application.port.FileDerivedDataCleanupPort;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.result.FilePermanentDeleteResult;
import com.group3.vitamins.file.application.result.FileRenameResult;
import com.group3.vitamins.file.application.result.FileRestoreResult;
import com.group3.vitamins.file.application.result.FileTrashResult;
import com.group3.vitamins.file.application.service.FileCommandService;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.model.UploadStatus;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
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
    private FileVersionRepository fileVersionRepository;
    private FileQueryPort fileQueryPort;
    private BlockCatalogPort blockCatalogPort;
    private StepAccessUseCase stepAccessUseCase;
    private ApprovalLockQueryPort approvalLockQueryPort;
    private FileStoragePort fileStoragePort;
    private FileDerivedDataCleanupPort fileDerivedDataCleanupPort;
    private DomainEventPublisher domainEventPublisher;
    private FileCommandService service;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(FileRepository.class);
        fileVersionRepository = Mockito.mock(FileVersionRepository.class);
        fileQueryPort = Mockito.mock(FileQueryPort.class);
        blockCatalogPort = Mockito.mock(BlockCatalogPort.class);
        stepAccessUseCase = Mockito.mock(StepAccessUseCase.class);
        approvalLockQueryPort = Mockito.mock(ApprovalLockQueryPort.class);
        fileStoragePort = Mockito.mock(FileStoragePort.class);
        fileDerivedDataCleanupPort = Mockito.mock(FileDerivedDataCleanupPort.class);
        domainEventPublisher = Mockito.mock(DomainEventPublisher.class);
        service = new FileCommandService(
                fileRepository, fileVersionRepository, fileQueryPort, blockCatalogPort,
                stepAccessUseCase, approvalLockQueryPort, fileStoragePort, fileDerivedDataCleanupPort,
                domainEventPublisher);
    }

    // ---- 헬퍼 ---------------------------------------------------------------

    private static final int VERSION = 3;

    private File activeFile() {
        return File.restore(FILE_ID, PROJECT_ID, "제안서", USER, null, VERSION);
    }

    private File trashedFile() {
        return File.restore(FILE_ID, PROJECT_ID, "제안서", USER, LocalDateTime.now(), VERSION);
    }

    private FileVersion version(long versionId, String storageKey) {
        return FileVersion.restore(versionId, FILE_ID, 1, UploadStatus.COMPLETED, storageKey,
                "제안서.pdf", "pdf", "application/pdf", 100L, null, null, null,
                USER, "김철수", "사업기획팀", "팀장", LocalDateTime.now(), null, null);
    }

    /** 문서 → 블록 → 스텝 편집 권한(EDITOR) 경로 스텁. */
    private void stubEditable() {
        when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
        when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
        when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.EDITOR));
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }

    /** 발행된 활동 로그 이벤트 1건을 캡처한다. */
    private ActivityOccurredEvent captureEvent() {
        ArgumentCaptor<ActivityOccurredEvent> captor = ArgumentCaptor.forClass(ActivityOccurredEvent.class);
        verify(domainEventPublisher).publish(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("§4 문서명 수정")
    class Rename {

        private RenameFileCommand cmd(String name) {
            return new RenameFileCommand(FILE_ID, name, VERSION, false, USER, ROLE);
        }

        private RenameFileCommand cmd(String name, int version, boolean overwrite) {
            return new RenameFileCommand(FILE_ID, name, version, overwrite, USER, ROLE);
        }

        @Test
        @DisplayName("표시명을 바꾸고 저장한다 — version 은 +1 되어 돌아온다")
        void renames() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubEditable();
            when(fileRepository.renameIfVersionMatches(FILE_ID, "제안서_최종", VERSION)).thenReturn(1);

            FileRenameResult result = service.rename(cmd("  제안서_최종  "));

            assertThat(result.name()).isEqualTo("제안서_최종"); // 앞뒤 공백은 정리한다
            assertThat(result.version()).isEqualTo(VERSION + 1); // 성공하면 다음 저장용 새 버전

            // 활동 로그: MODIFY + fileName 변경 전·후
            ActivityOccurredEvent event = captureEvent();
            assertThat(event.action()).isEqualTo(ActivityLogAction.MODIFY);
            assertThat(event.blockId()).isEqualTo(BLOCK_ID);
            assertThat(event.resourceId()).isEqualTo(FILE_ID);
            assertThat(event.resourceName()).isEqualTo("제안서_최종");
            assertThat(event.changes()).singleElement()
                    .satisfies(c -> {
                        assertThat(c.field()).isEqualTo("fileName");
                        assertThat(c.beforeValue()).isEqualTo("제안서");
                        assertThat(c.afterValue()).isEqualTo("제안서_최종");
                    });
        }

        @Test
        @DisplayName("이름이 그대로면 저장은 해도 로그는 남기지 않는다")
        void sameNameNoLog() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile())); // 이름 "제안서"
            stubEditable();
            when(fileRepository.renameIfVersionMatches(FILE_ID, "제안서", VERSION)).thenReturn(1);

            service.rename(cmd("제안서"));

            verify(domainEventPublisher, never()).publish(any());
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
            when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenThrow(new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));

            assertThatThrownBy(() -> service.rename(cmd("새이름")))
                    .satisfies(hasCode(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED));
        }

        @Test
        @DisplayName("그 사이 남이 먼저 저장했으면(0행) FILE_VERSION_CONFLICT")
        void conflict() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubEditable();
            when(fileRepository.renameIfVersionMatches(FILE_ID, "새이름", VERSION)).thenReturn(0); // 0행 = 충돌

            assertThatThrownBy(() -> service.rename(cmd("새이름")))
                    .satisfies(hasCode(FileErrorCode.FILE_VERSION_CONFLICT));
            verify(domainEventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("덮어쓰기는 잠금 시점 이름·버전을 쓴다 — 초기 조회 이후 남이 바꾼 이름을 before 로 기록하고 current+1 을 돌려준다")
        void overwriteUsesLockedNameAndVersion() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile())); // 초기 조회 이름 "제안서"·version 3
            stubEditable();
            // 초기 조회~잠금 사이 남이 "제안서_B" 로 바꿔 version 이 7 이 됐다
            when(fileRepository.lockForOverwrite(FILE_ID))
                    .thenReturn(Optional.of(File.restore(FILE_ID, PROJECT_ID, "제안서_B", USER, null, 7)));
            when(fileRepository.renameIfVersionMatches(FILE_ID, "강제", 7)).thenReturn(1);

            // 클라가 낡은 1 을 보냈어도 잠근 현재값 7 로 저장, 응답은 8
            FileRenameResult result = service.rename(cmd("강제", 1, true));

            assertThat(result.version()).isEqualTo(8);
            verify(fileRepository).renameIfVersionMatches(FILE_ID, "강제", 7); // 클라값(1) 아님

            // 활동 로그 before 는 초기 조회("제안서")가 아니라 잠금 시점("제안서_B")
            ActivityOccurredEvent event = captureEvent();
            assertThat(event.changes()).singleElement().satisfies(c -> {
                assertThat(c.beforeValue()).isEqualTo("제안서_B");
                assertThat(c.afterValue()).isEqualTo("강제");
            });
        }

        @Test
        @DisplayName("덮어쓰기 대상이 그새 삭제됐으면 FILE_NOT_FOUND")
        void overwriteGoneWhileLocking() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubEditable();
            when(fileRepository.lockForOverwrite(FILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.rename(cmd("강제", 1, true)))
                    .satisfies(hasCode(FileErrorCode.FILE_NOT_FOUND));
        }

        @Test
        @DisplayName("version 이 1 미만이면(누락 포함) overwrite 여부와 무관하게 FILE_INVALID_REQUEST")
        void invalidVersion() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubEditable();

            assertThatThrownBy(() -> service.rename(cmd("새이름", 0, false)))
                    .satisfies(hasCode(FileErrorCode.FILE_INVALID_REQUEST));
            assertThatThrownBy(() -> service.rename(cmd("새이름", 0, true)))
                    .satisfies(hasCode(FileErrorCode.FILE_INVALID_REQUEST));
            verify(fileRepository, never()).lockForOverwrite(any());
            verify(fileRepository, never()).renameIfVersionMatches(any(), any(), anyInt());
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

            // 활동 로그: 휴지통 이동 = DELETE
            ActivityOccurredEvent event = captureEvent();
            assertThat(event.action()).isEqualTo(ActivityLogAction.DELETE);
            assertThat(event.blockId()).isEqualTo(BLOCK_ID);
            assertThat(event.resourceId()).isEqualTo(FILE_ID);
            assertThat(event.resourceName()).isEqualTo("제안서");
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
            when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenThrow(new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));

            assertThatThrownBy(() -> service.moveToTrash(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED));
        }
    }

    @Nested
    @DisplayName("§7 영구 삭제")
    class PermanentDelete {

        private PermanentDeleteFileCommand cmd(String confirmText) {
            return new PermanentDeleteFileCommand(FILE_ID, confirmText, USER, ROLE);
        }

        private void assertNoDeletion() {
            verify(fileDerivedDataCleanupPort, never()).cleanupByFileId(anyLong());
            verify(fileVersionRepository, never()).deleteByFileId(anyLong());
            verify(fileRepository, never()).deleteById(anyLong());
            verify(fileStoragePort, never()).deleteObjects(any());
        }

        @Test
        @DisplayName("파생데이터 정리 → 전 버전·file 삭제 → 저장소 삭제 순서로 지운다")
        void permanentlyDeletes() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));
            stubEditable();
            when(approvalLockQueryPort.existsAnyApprovalReference(FILE_ID)).thenReturn(false);
            when(fileVersionRepository.findByFileId(FILE_ID))
                    .thenReturn(List.of(version(1L, "key-1"), version(2L, "key-2")));
            when(fileStoragePort.deleteObjects(any())).thenReturn(2);

            FilePermanentDeleteResult result = service.permanentDelete(cmd("영구 삭제"));

            assertThat(result.fileId()).isEqualTo(FILE_ID);
            assertThat(result.deletedVersionCount()).isEqualTo(2);
            assertThat(result.storageDeletedCount()).isEqualTo(2);

            // ⭐ DB 삭제 前 파생데이터 정리 → 버전 → file(그다음 저장소) 순서가 지켜져야 한다(FK 안전).
            InOrder order = inOrder(
                    fileDerivedDataCleanupPort, fileVersionRepository, fileRepository, fileStoragePort);
            order.verify(fileDerivedDataCleanupPort).cleanupByFileId(FILE_ID);
            order.verify(fileVersionRepository).deleteByFileId(FILE_ID);
            order.verify(fileRepository).deleteById(FILE_ID);
            order.verify(fileStoragePort).deleteObjects(any());

            // 저장소 삭제엔 전 버전의 키가 넘어간다.
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<String>> keys = ArgumentCaptor.forClass(Collection.class);
            verify(fileStoragePort).deleteObjects(keys.capture());
            assertThat(keys.getValue()).containsExactlyInAnyOrder("key-1", "key-2");

            // ⭐ 함정 검증: blockId 는 file 삭제(block_file CASCADE) 前에 잡혀야 한다.
            InOrder blockOrder = inOrder(fileQueryPort, fileRepository);
            blockOrder.verify(fileQueryPort, Mockito.atLeastOnce()).findBlockIdByFileId(FILE_ID);
            blockOrder.verify(fileRepository).deleteById(FILE_ID);

            // 활동 로그: 영구 삭제 = PURGE, 삭제 전 blockId·파일명이 실린다.
            ActivityOccurredEvent event = captureEvent();
            assertThat(event.action()).isEqualTo(ActivityLogAction.PURGE);
            assertThat(event.blockId()).isEqualTo(BLOCK_ID);
            assertThat(event.resourceId()).isEqualTo(FILE_ID);
            assertThat(event.resourceName()).isEqualTo("제안서");
        }

        @Test
        @DisplayName("휴지통에 없으면 FILE_NOT_DELETED — 아무것도 지우지 않는다")
        void notInTrash() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubEditable();

            assertThatThrownBy(() -> service.permanentDelete(cmd("영구 삭제")))
                    .satisfies(hasCode(FileErrorCode.FILE_NOT_DELETED));
            assertNoDeletion();
        }

        @Test
        @DisplayName("확인 문자가 다르면 FILE_CONFIRM_TEXT_MISMATCH — 아무것도 지우지 않는다")
        void confirmTextMismatch() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));
            stubEditable();

            assertThatThrownBy(() -> service.permanentDelete(cmd("삭제")))
                    .satisfies(hasCode(FileErrorCode.FILE_CONFIRM_TEXT_MISMATCH));
            assertNoDeletion();
        }

        @Test
        @DisplayName("결재가 버전을 참조하면 FILE_APPROVAL_REFERENCED(409) — 아무것도 지우지 않는다")
        void approvalReferenced() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));
            stubEditable();
            when(approvalLockQueryPort.existsAnyApprovalReference(FILE_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.permanentDelete(cmd("영구 삭제")))
                    .satisfies(hasCode(FileErrorCode.FILE_APPROVAL_REFERENCED));
            assertNoDeletion();
        }

        @Test
        @DisplayName("없는 문서면 FILE_NOT_FOUND")
        void notFound() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.permanentDelete(cmd("영구 삭제")))
                    .satisfies(hasCode(FileErrorCode.FILE_NOT_FOUND));
        }

        @Test
        @DisplayName("편집 권한이 없으면 FILE_EDIT_PERMISSION_REQUIRED")
        void notEditable() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));
            when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
            when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenThrow(new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));

            assertThatThrownBy(() -> service.permanentDelete(cmd("영구 삭제")))
                    .satisfies(hasCode(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED));
            assertNoDeletion();
        }
    }

    @Nested
    @DisplayName("§6 휴지통 복구")
    class Restore {

        private RestoreFileCommand cmd() {
            return new RestoreFileCommand(FILE_ID, USER, ROLE);
        }

        /** 삭제 무시 스텝 조회 + EDITOR 권한 스텁(복구 경로 전용). */
        private void stubRestorable() {
            when(fileQueryPort.findStepIdByFileIdIncludingDeletedBlock(FILE_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.EDITOR));
        }

        @Test
        @DisplayName("살아있는 블록으로 복구 — blockId 채우고 blockDeleted=false")
        void restoresToLiveBlock() {
            File file = trashedFile();
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
            stubRestorable();
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
            when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));

            FileRestoreResult result = service.restore(cmd());

            assertThat(file.isDeleted()).isFalse();
            assertThat(result.blockId()).isEqualTo(BLOCK_ID);
            assertThat(result.blockDeleted()).isFalse();

            // 활동 로그: 복원 = RESTORE
            ActivityOccurredEvent event = captureEvent();
            assertThat(event.action()).isEqualTo(ActivityLogAction.RESTORE);
            assertThat(event.blockId()).isEqualTo(BLOCK_ID);
            assertThat(event.resourceId()).isEqualTo(FILE_ID);
        }

        @Test
        @DisplayName("블록이 삭제됐어도 복구 — blockId=null·blockDeleted=true")
        void restoresWhenBlockDeleted() {
            File file = trashedFile();
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
            stubRestorable();
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
            // 원래 블록이 soft delete 돼 attachable 해석은 비어있다.
            when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.empty());

            FileRestoreResult result = service.restore(cmd());

            assertThat(file.isDeleted()).isFalse();
            assertThat(result.blockId()).isNull();
            assertThat(result.blockDeleted()).isTrue();

            // ⭐ 함정 검증: 결과는 blockDeleted=true 라도, 로그는 원래(soft delete 된) 블록으로 남는다.
            // block_file 링크 행은 블록 삭제에도 남아 원래 blockId 를 돌려주기 때문이다.
            ActivityOccurredEvent event = captureEvent();
            assertThat(event.action()).isEqualTo(ActivityLogAction.RESTORE);
            assertThat(event.blockId()).isEqualTo(BLOCK_ID);
        }

        @Test
        @DisplayName("복구는 됐지만 블록 링크 자체가 사라졌으면 로그는 건너뛴다(작업은 성공)")
        void restoreWithNoBlockLinkSkipsLog() {
            File file = trashedFile();
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
            stubRestorable();
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // 링크 행 자체가 없다 → linkedBlockId=null → 이벤트 blockId 가 null 이라 발행을 건너뛴다.
            when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.empty());

            FileRestoreResult result = service.restore(cmd());

            assertThat(file.isDeleted()).isFalse();
            assertThat(result.blockDeleted()).isTrue();
            verify(domainEventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("휴지통에 없으면 FILE_NOT_DELETED — 저장하지 않는다")
        void notInTrash() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            stubRestorable();

            assertThatThrownBy(() -> service.restore(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_NOT_DELETED));
            verify(fileRepository, never()).save(any());
        }

        @Test
        @DisplayName("없는 문서면 FILE_NOT_FOUND")
        void notFound() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.restore(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_NOT_FOUND));
        }

        @Test
        @DisplayName("편집 권한이 없으면 FILE_EDIT_PERMISSION_REQUIRED — 저장하지 않는다")
        void notEditable() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));
            when(fileQueryPort.findStepIdByFileIdIncludingDeletedBlock(FILE_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenThrow(new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));

            assertThatThrownBy(() -> service.restore(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED));
            verify(fileRepository, never()).save(any());
        }

        @Test
        @DisplayName("블록 링크가 아예 없으면 FILE_BLOCK_NOT_FOUND")
        void blockLinkMissing() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));
            when(fileQueryPort.findStepIdByFileIdIncludingDeletedBlock(FILE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.restore(cmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("D안 · 블록 삭제 시 파일 휴지통 이동")
    class TrashByBlockDeletion {

        private static final Long BLOCK = 12L;
        private static final Long F1 = 101L;
        private static final Long F2 = 102L;

        private File active(Long id, String name) {
            return File.restore(id, PROJECT_ID, name, USER, null, VERSION);
        }

        @Test
        @DisplayName("블록의 활성 파일을 모두 휴지통으로 — 권한 재검사 없이 파일별 DELETE 로그 발행")
        void trashesAllActiveFiles() {
            File f1 = active(F1, "제안서");
            File f2 = active(F2, "계약서");
            when(fileQueryPort.findActiveFileIdsByBlockId(BLOCK)).thenReturn(List.of(F1, F2));
            when(approvalLockQueryPort.findInProgressApproval(anyLong())).thenReturn(Optional.empty());
            when(fileRepository.findById(F1)).thenReturn(Optional.of(f1));
            when(fileRepository.findById(F2)).thenReturn(Optional.of(f2));
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int trashed = service.trashByBlockDeletion(BLOCK, USER);

            assertThat(trashed).isEqualTo(2);
            assertThat(f1.isDeleted()).isTrue();
            assertThat(f2.isDeleted()).isTrue();
            // 블록 삭제 경로는 스텝 편집권한을 재검사하지 않는다(상위에서 이미 판정).
            verify(stepAccessUseCase, never()).requireEditable(anyLong(), any(), any());
            // 파일별 DELETE 로그(blockId 로) 발행.
            ArgumentCaptor<ActivityOccurredEvent> captor = ArgumentCaptor.forClass(ActivityOccurredEvent.class);
            verify(domainEventPublisher, Mockito.times(2)).publish(captor.capture());
            assertThat(captor.getAllValues()).allSatisfy(e -> {
                assertThat(e.action()).isEqualTo(ActivityLogAction.DELETE);
                assertThat(e.blockId()).isEqualTo(BLOCK);
            });
        }

        @Test
        @DisplayName("활성 파일이 없으면 0 — 저장·로그 없음")
        void emptyReturnsZero() {
            when(fileQueryPort.findActiveFileIdsByBlockId(BLOCK)).thenReturn(List.of());

            assertThat(service.trashByBlockDeletion(BLOCK, USER)).isZero();
            verify(fileRepository, never()).save(any());
            verify(domainEventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("진행 중 결재 참조 파일이 하나라도 있으면 FILE_APPROVAL_IN_PROGRESS — 아무 파일도 트래시하지 않는다")
        void approvalLockedBlocksAll() {
            when(fileQueryPort.findActiveFileIdsByBlockId(BLOCK)).thenReturn(List.of(F1, F2));
            when(approvalLockQueryPort.findInProgressApproval(F1)).thenReturn(Optional.empty());
            when(approvalLockQueryPort.findInProgressApproval(F2))
                    .thenReturn(Optional.of(new ApprovalLockQueryPort.InProgressApproval(9L, "계약서 결재")));

            assertThatThrownBy(() -> service.trashByBlockDeletion(BLOCK, USER))
                    .satisfies(hasCode(FileErrorCode.FILE_APPROVAL_IN_PROGRESS))
                    .hasMessageContaining("계약서 결재");
            // 선검사에서 막혀 어떤 파일도 저장·발행되지 않는다.
            verify(fileRepository, never()).save(any());
            verify(domainEventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("이미 휴지통이거나 사라진 파일은 건너뛴다(멱등)")
        void skipsGoneOrDeleted() {
            File f1 = active(F1, "제안서");
            when(fileQueryPort.findActiveFileIdsByBlockId(BLOCK)).thenReturn(List.of(F1, F2));
            when(approvalLockQueryPort.findInProgressApproval(anyLong())).thenReturn(Optional.empty());
            when(fileRepository.findById(F1)).thenReturn(Optional.of(f1));
            when(fileRepository.findById(F2)).thenReturn(Optional.empty()); // 그새 사라짐
            when(fileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.trashByBlockDeletion(BLOCK, USER)).isEqualTo(1);
            assertThat(f1.isDeleted()).isTrue();
        }
    }
}
