package com.group3.vitamins.file.application;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;

import com.group3.vitamins.file.application.command.CompleteFileUploadCommand;
import com.group3.vitamins.file.application.command.StartFileUploadCommand;
import com.group3.vitamins.file.application.port.BlockCatalogPort;
import com.group3.vitamins.file.application.port.FileIndexTriggerPort;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.port.PdfPageCounterPort;
import com.group3.vitamins.file.application.port.UploaderLookupPort;
import com.group3.vitamins.file.application.result.FileUploadStartResult;
import com.group3.vitamins.file.application.result.FileVersionDetailResult;
import com.group3.vitamins.file.application.service.FileUploadService;
import com.group3.vitamins.file.application.service.FileVersionFailureRecorder;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.model.UploadStatus;
import com.group3.vitamins.file.domain.repository.BlockFileRepository;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FileUploadService 업로드 시작·완료 통보")
class FileUploadServiceTest {

    private static final Long BLOCK_ID = 12L;
    private static final Long STEP_ID = 5L;
    private static final Long PROJECT_ID = 100L;
    private static final String USER = "EMP001";
    private static final String ROLE = "MEMBER";

    private BlockCatalogPort blockCatalogPort;
    private StepAccessUseCase stepAccessUseCase;
    private FileRepository fileRepository;
    private FileVersionRepository fileVersionRepository;
    private BlockFileRepository blockFileRepository;
    private FileQueryPort fileQueryPort;
    private UploaderLookupPort uploaderLookupPort;
    private FileStoragePort fileStoragePort;
    private PdfPageCounterPort pdfPageCounterPort;
    private FileVersionFailureRecorder failureRecorder;
    private FileIndexTriggerPort fileIndexTriggerPort;
    private FileUploadService service;

    @BeforeEach
    void setUp() {
        blockCatalogPort = Mockito.mock(BlockCatalogPort.class);
        stepAccessUseCase = Mockito.mock(StepAccessUseCase.class);
        fileRepository = Mockito.mock(FileRepository.class);
        fileVersionRepository = Mockito.mock(FileVersionRepository.class);
        blockFileRepository = Mockito.mock(BlockFileRepository.class);
        fileQueryPort = Mockito.mock(FileQueryPort.class);
        uploaderLookupPort = Mockito.mock(UploaderLookupPort.class);
        fileStoragePort = Mockito.mock(FileStoragePort.class);
        pdfPageCounterPort = Mockito.mock(PdfPageCounterPort.class);
        failureRecorder = Mockito.mock(FileVersionFailureRecorder.class);
        fileIndexTriggerPort = Mockito.mock(FileIndexTriggerPort.class);
        CurrentCompanyIdProvider currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(1L);
        service = new FileUploadService(
                blockCatalogPort, stepAccessUseCase, fileRepository, fileVersionRepository,
                blockFileRepository, fileQueryPort, uploaderLookupPort, fileStoragePort, pdfPageCounterPort,
                failureRecorder, fileIndexTriggerPort, currentCompanyIdProvider);
    }

    private void stubBlockAndEditable() {
        when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
        when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.EDITOR));
    }

    private void stubUploader() {
        when(uploaderLookupPort.findByUserId(USER)).thenReturn(
                Optional.of(new UploaderLookupPort.UploaderSnapshot("이영희", "제안팀", "선임연구원")));
    }

    private StartFileUploadCommand startCmd(String fileName, long size, String name, Long fileId, boolean allowDup) {
        return new StartFileUploadCommand(BLOCK_ID, fileName, size, "application/pdf",
                name, fileId, "초안", allowDup, USER, ROLE);
    }

    private FileVersion uploadingVersion(Long id, Long fileId, int versionNo, String ext) {
        return FileVersion.restore(id, fileId, versionNo, UploadStatus.UPLOADING,
                "companies/1/projects/100/files/31/versions/" + versionNo + "/uuid." + ext,
                "제안서_v" + versionNo + "." + ext, ext, "application/pdf", 5000L, null, null, "초안",
                USER, "이영희", "제안팀", "선임연구원", null, null);
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Nested
    @DisplayName("업로드 시작")
    class Start {

        @Test
        @DisplayName("새 문서 — File 저장·block_file 링크 후 presigned 발급")
        void startsNewDocument() {
            stubBlockAndEditable();
            stubUploader();
            when(fileQueryPort.existsActiveNameInBlock(BLOCK_ID, "제안서")).thenReturn(false);
            when(fileRepository.save(any())).thenReturn(File.restore(31L, PROJECT_ID, "제안서", USER, null));
            when(fileVersionRepository.save(any())).thenReturn(uploadingVersion(74L, 31L, 1, "pdf"));
            when(fileStoragePort.presignUpload(anyString(), anyString(), anyLong()))
                    .thenReturn(new FileStoragePort.PresignedUrl("https://s3/put", Instant.now()));

            FileUploadStartResult result = service.startUpload(
                    startCmd("제안서_v1.pdf", 5000L, "제안서", null, false));

            assertThat(result.fileId()).isEqualTo(31L);
            assertThat(result.fileVersionId()).isEqualTo(74L);
            assertThat(result.versionNo()).isEqualTo(1);
            assertThat(result.uploadUrl()).isEqualTo("https://s3/put");
            verify(blockFileRepository, times(1)).link(BLOCK_ID, 31L, USER);
        }

        @Test
        @DisplayName("새 버전 — 기존 문서에 maxVersionNo+1, 링크는 다시 만들지 않는다")
        void startsNewVersion() {
            stubBlockAndEditable();
            stubUploader();
            when(fileRepository.findById(31L)).thenReturn(Optional.of(File.restore(31L, PROJECT_ID, "제안서", USER, null)));
            when(fileVersionRepository.findMaxVersionNo(31L)).thenReturn(1);
            when(fileVersionRepository.save(any())).thenReturn(uploadingVersion(75L, 31L, 2, "pdf"));
            when(fileStoragePort.presignUpload(anyString(), anyString(), anyLong()))
                    .thenReturn(new FileStoragePort.PresignedUrl("https://s3/put", Instant.now()));

            FileUploadStartResult result = service.startUpload(
                    startCmd("제안서_v2.pdf", 5000L, null, 31L, false));

            assertThat(result.versionNo()).isEqualTo(2);
            verify(fileRepository, never()).save(any());
            verify(blockFileRepository, never()).link(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("블록이 없거나 삭제되었으면 FILE_BLOCK_NOT_FOUND")
        void blockNotFound() {
            when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.startUpload(startCmd("a.pdf", 5000L, null, null, false)))
                    .satisfies(hasCode(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        }

        @Test
        @DisplayName("편집 권한이 없으면 FILE_EDIT_PERMISSION_REQUIRED")
        void notEditable() {
            when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenThrow(new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));

            assertThatThrownBy(() -> service.startUpload(startCmd("a.pdf", 5000L, null, null, false)))
                    .satisfies(hasCode(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED));
        }

        @Test
        @DisplayName("50MB 초과면 FILE_SIZE_EXCEEDED")
        void sizeExceeded() {
            stubBlockAndEditable();

            assertThatThrownBy(() -> service.startUpload(
                    startCmd("a.pdf", 50L * 1024 * 1024 + 1, null, null, false)))
                    .satisfies(hasCode(FileErrorCode.FILE_SIZE_EXCEEDED));
        }

        @Test
        @DisplayName("실행 파일 확장자면 FILE_EXTENSION_BLOCKED")
        void extensionBlocked() {
            stubBlockAndEditable();

            assertThatThrownBy(() -> service.startUpload(startCmd("malware.exe", 5000L, null, null, false)))
                    .satisfies(hasCode(FileErrorCode.FILE_EXTENSION_BLOCKED));
        }

        @Test
        @DisplayName("동명 문서가 있고 allowDuplicateName=false 면 FILE_NAME_DUPLICATED")
        void nameDuplicated() {
            stubBlockAndEditable();
            when(fileQueryPort.existsActiveNameInBlock(BLOCK_ID, "제안서")).thenReturn(true);

            assertThatThrownBy(() -> service.startUpload(startCmd("제안서_v1.pdf", 5000L, "제안서", null, false)))
                    .satisfies(hasCode(FileErrorCode.FILE_NAME_DUPLICATED));
            verify(fileRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("완료 통보")
    class Complete {

        private CompleteFileUploadCommand completeCmd() {
            return new CompleteFileUploadCommand(74L, null, USER, ROLE);
        }

        private void stubPermissionForComplete() {
            when(fileQueryPort.findBlockIdByFileId(31L)).thenReturn(Optional.of(BLOCK_ID));
            when(blockCatalogPort.resolveAttachableBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireEditable(STEP_ID, USER, ROLE))
                    .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.EDITOR));
        }

        @Test
        @DisplayName("PDF — HEAD 검증 통과·페이지수 추출 후 COMPLETED, 상세 반환")
        void completesPdf() {
            FileVersion version = uploadingVersion(74L, 31L, 1, "pdf");
            when(fileVersionRepository.findById(74L)).thenReturn(Optional.of(version));
            stubPermissionForComplete();
            when(fileStoragePort.head(anyString())).thenReturn(Optional.of(new FileStoragePort.StoredObject(5000L)));
            when(fileStoragePort.getObject(anyString())).thenReturn(new byte[]{1, 2, 3});
            when(pdfPageCounterPort.countPages(any())).thenReturn(Optional.of(42));
            when(fileVersionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(fileRepository.findById(31L)).thenReturn(Optional.of(File.restore(31L, PROJECT_ID, "제안서", USER, null)));

            FileVersionDetailResult result = service.completeUpload(completeCmd());

            assertThat(result.pageCount()).isEqualTo(42);
            assertThat(result.name()).isEqualTo("제안서");
            assertThat(version.getUploadStatus()).isEqualTo(UploadStatus.COMPLETED);
            verify(fileIndexTriggerPort, times(1)).triggerIndexing(74L);
        }

        @Test
        @DisplayName("이미 완료된 버전이면 FILE_ALREADY_COMPLETED")
        void alreadyCompleted() {
            FileVersion completed = FileVersion.restore(74L, 31L, 1, UploadStatus.COMPLETED,
                    "k", "a.pdf", "pdf", null, 5000L, null, 3, null, USER, "이영희", null, null, null, null);
            when(fileVersionRepository.findById(74L)).thenReturn(Optional.of(completed));

            assertThatThrownBy(() -> service.completeUpload(completeCmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_ALREADY_COMPLETED));
        }

        @Test
        @DisplayName("저장소에 객체가 없으면 버전을 FAILED 로 바꾸고 FILE_OBJECT_NOT_FOUND")
        void objectNotFound() {
            FileVersion version = uploadingVersion(74L, 31L, 1, "pdf");
            when(fileVersionRepository.findById(74L)).thenReturn(Optional.of(version));
            stubPermissionForComplete();
            when(fileStoragePort.head(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeUpload(completeCmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_OBJECT_NOT_FOUND));
            // FAILED 전이는 REQUIRES_NEW 레코더가 별도 트랜잭션에서 확정 저장한다(롤백 회피).
            verify(failureRecorder, times(1)).markFailed(version);
            verify(fileIndexTriggerPort, never()).triggerIndexing(anyLong());
        }

        @Test
        @DisplayName("크기가 다르면 FILE_SIZE_MISMATCH")
        void sizeMismatch() {
            FileVersion version = uploadingVersion(74L, 31L, 1, "pdf");
            when(fileVersionRepository.findById(74L)).thenReturn(Optional.of(version));
            stubPermissionForComplete();
            when(fileStoragePort.head(anyString())).thenReturn(Optional.of(new FileStoragePort.StoredObject(9999L)));

            assertThatThrownBy(() -> service.completeUpload(completeCmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_SIZE_MISMATCH));
            // 크기 불일치도 객체 없음과 대칭으로 FAILED 를 기록한다.
            verify(failureRecorder, times(1)).markFailed(version);
            verify(fileIndexTriggerPort, never()).triggerIndexing(anyLong());
        }

        @Test
        @DisplayName("버전이 없으면 FILE_VERSION_NOT_FOUND")
        void versionNotFound() {
            when(fileVersionRepository.findById(74L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeUpload(completeCmd()))
                    .satisfies(hasCode(FileErrorCode.FILE_VERSION_NOT_FOUND));
        }
    }
}
