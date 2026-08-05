package com.group3.vitamins.file.application;

import com.group3.vitamins.file.application.port.BlockCatalogPort;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.port.PdfPreviewPort;
import com.group3.vitamins.file.application.result.BlockFileListResult;
import com.group3.vitamins.file.application.result.BlockFileProjection;
import com.group3.vitamins.file.application.result.DownloadUrlResult;
import com.group3.vitamins.file.application.result.FilePreviewResult;
import com.group3.vitamins.file.application.result.FileVersionProjection;
import com.group3.vitamins.file.application.result.FileVersionSingleResult;
import com.group3.vitamins.file.application.result.VersionHistoryResult;
import com.group3.vitamins.file.application.service.FileQueryService;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.model.UploadStatus;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FileQueryService 조회 5종")
class FileQueryServiceTest {

    private static final Long FILE_VERSION_ID = 74L;
    private static final Long FILE_ID = 31L;
    private static final Long BLOCK_ID = 12L;
    private static final Long STEP_ID = 5L;
    private static final Long PROJECT_ID = 100L;
    private static final String USER = "EMP001";
    private static final String ROLE = "MEMBER";

    private FileVersionRepository fileVersionRepository;
    private FileRepository fileRepository;
    private FileQueryPort fileQueryPort;
    private BlockCatalogPort blockCatalogPort;
    private StepAccessUseCase stepAccessUseCase;
    private FileStoragePort fileStoragePort;
    private PdfPreviewPort pdfPreviewPort;
    private FileQueryService service;

    @BeforeEach
    void setUp() {
        fileVersionRepository = Mockito.mock(FileVersionRepository.class);
        fileRepository = Mockito.mock(FileRepository.class);
        fileQueryPort = Mockito.mock(FileQueryPort.class);
        blockCatalogPort = Mockito.mock(BlockCatalogPort.class);
        stepAccessUseCase = Mockito.mock(StepAccessUseCase.class);
        fileStoragePort = Mockito.mock(FileStoragePort.class);
        pdfPreviewPort = Mockito.mock(PdfPreviewPort.class);
        service = new FileQueryService(
                fileVersionRepository, fileRepository, fileQueryPort,
                blockCatalogPort, stepAccessUseCase, fileStoragePort, pdfPreviewPort);
    }

    // ---- 헬퍼 ---------------------------------------------------------------

    private File activeFile() {
        return File.restore(FILE_ID, PROJECT_ID, "제안서", USER, null);
    }

    private File trashedFile() {
        return File.restore(FILE_ID, PROJECT_ID, "제안서", USER, LocalDateTime.now());
    }

    private FileVersion version(int versionNo, UploadStatus status, String ext) {
        return FileVersion.restore(FILE_VERSION_ID, FILE_ID, versionNo, status,
                "projects/100/files/31/versions/" + versionNo + "/uuid." + ext,
                "제안서_v" + versionNo + "." + ext, ext, "application/pdf", 5000L, null, 42, "초안",
                USER, "이영희", "제안팀", "선임연구원", LocalDateTime.now(), null);
    }

    /** 버전 → 문서 → 블록 → 스텝 접근 경로 스텁 (VIEWER 이상). */
    private void stubVersionAccess(FileVersion v, File file, MemberPermission permission) {
        when(fileVersionRepository.findById(FILE_VERSION_ID)).thenReturn(Optional.of(v));
        when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(file));
        when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
        when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
        when(stepAccessUseCase.requireAccess(STEP_ID, USER, ROLE))
                .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, permission));
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Nested
    @DisplayName("§9 다운로드 URL")
    class Download {

        @Test
        @DisplayName("완료된 버전이면 presigned 다운로드 URL 을 발급한다")
        void issuesUrl() {
            stubVersionAccess(version(1, UploadStatus.COMPLETED, "pdf"), activeFile(), MemberPermission.VIEWER);
            when(fileStoragePort.presignDownload(anyString(), anyString()))
                    .thenReturn(new FileStoragePort.PresignedUrl("https://s3/get", Instant.now()));

            DownloadUrlResult result = service.getDownloadUrl(FILE_VERSION_ID, USER, ROLE);

            assertThat(result.fileVersionId()).isEqualTo(FILE_VERSION_ID);
            assertThat(result.downloadUrl()).isEqualTo("https://s3/get");
            assertThat(result.sizeBytes()).isEqualTo(5000L);
        }

        @Test
        @DisplayName("문서가 휴지통이면 FILE_VERSION_NOT_FOUND")
        void deletedFile() {
            stubVersionAccess(version(1, UploadStatus.COMPLETED, "pdf"), trashedFile(), MemberPermission.VIEWER);

            assertThatThrownBy(() -> service.getDownloadUrl(FILE_VERSION_ID, USER, ROLE))
                    .satisfies(hasCode(FileErrorCode.FILE_VERSION_NOT_FOUND));
            verify(fileStoragePort, never()).presignDownload(anyString(), anyString());
        }

        @Test
        @DisplayName("업로드 미완료 버전이면 FILE_UPLOAD_NOT_COMPLETED")
        void notCompleted() {
            stubVersionAccess(version(1, UploadStatus.UPLOADING, "pdf"), activeFile(), MemberPermission.VIEWER);

            assertThatThrownBy(() -> service.getDownloadUrl(FILE_VERSION_ID, USER, ROLE))
                    .satisfies(hasCode(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED));
        }

        @Test
        @DisplayName("버전이 없으면 FILE_VERSION_NOT_FOUND")
        void versionNotFound() {
            when(fileVersionRepository.findById(FILE_VERSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDownloadUrl(FILE_VERSION_ID, USER, ROLE))
                    .satisfies(hasCode(FileErrorCode.FILE_VERSION_NOT_FOUND));
        }

        @Test
        @DisplayName("열람 권한이 없으면 FILE_ACCESS_PERMISSION_REQUIRED")
        void noAccess() {
            when(fileVersionRepository.findById(FILE_VERSION_ID))
                    .thenReturn(Optional.of(version(1, UploadStatus.COMPLETED, "pdf")));
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
            when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireAccess(STEP_ID, USER, ROLE))
                    .thenThrow(new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));

            assertThatThrownBy(() -> service.getDownloadUrl(FILE_VERSION_ID, USER, ROLE))
                    .satisfies(hasCode(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED));
        }
    }

    @Nested
    @DisplayName("§11 버전 단건")
    class VersionSingle {

        @Test
        @DisplayName("최신 차수면 latest=true 로 반환한다")
        void latestVersion() {
            stubVersionAccess(version(2, UploadStatus.COMPLETED, "pdf"), activeFile(), MemberPermission.VIEWER);
            when(fileVersionRepository.findMaxVersionNo(FILE_ID)).thenReturn(2);

            FileVersionSingleResult result = service.getVersion(FILE_VERSION_ID, USER, ROLE);

            assertThat(result.versionNo()).isEqualTo(2);
            assertThat(result.latest()).isTrue();
            assertThat(result.latestVersionNo()).isEqualTo(2);
            assertThat(result.fileDeleted()).isFalse();
        }

        @Test
        @DisplayName("휴지통 문서의 버전도 반환하되 fileDeleted=true 로 표시한다 (§11)")
        void deletedFileStillReturned() {
            stubVersionAccess(version(1, UploadStatus.COMPLETED, "pdf"), trashedFile(), MemberPermission.VIEWER);
            when(fileVersionRepository.findMaxVersionNo(FILE_ID)).thenReturn(2);

            FileVersionSingleResult result = service.getVersion(FILE_VERSION_ID, USER, ROLE);

            assertThat(result.latest()).isFalse();
            assertThat(result.fileDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("§8 버전 이력")
    class History {

        private FileVersionProjection projection(Long id, int versionNo) {
            return new FileVersionProjection(id, versionNo, "제안서_v" + versionNo + ".pdf", "pdf",
                    5000L, 42, "초안", "이영희", "제안팀", "선임연구원", LocalDateTime.now());
        }

        @Test
        @DisplayName("완료 버전을 차수 내림차순으로 반환하고 첫 행에 latest=true")
        void listsHistory() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(activeFile()));
            when(fileQueryPort.findBlockIdByFileId(FILE_ID)).thenReturn(Optional.of(BLOCK_ID));
            when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireAccess(STEP_ID, USER, ROLE))
                    .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.VIEWER));
            when(fileQueryPort.findCompletedVersions(FILE_ID))
                    .thenReturn(List.of(projection(75L, 2), projection(74L, 1)));

            VersionHistoryResult result = service.getVersionHistory(FILE_ID, USER, ROLE);

            assertThat(result.versionCount()).isEqualTo(2);
            assertThat(result.content().get(0).versionNo()).isEqualTo(2);
            assertThat(result.content().get(0).latest()).isTrue();
            assertThat(result.content().get(1).latest()).isFalse();
        }

        @Test
        @DisplayName("문서가 없거나 휴지통이면 FILE_NOT_FOUND")
        void fileNotFound() {
            when(fileRepository.findById(FILE_ID)).thenReturn(Optional.of(trashedFile()));

            assertThatThrownBy(() -> service.getVersionHistory(FILE_ID, USER, ROLE))
                    .satisfies(hasCode(FileErrorCode.FILE_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("§3 블록 파일 목록")
    class BlockFiles {

        private BlockFileProjection projection() {
            return new BlockFileProjection(FILE_ID, "제안서", 74L, 1, 1,
                    "제안서_v1.pdf", "pdf", 5000L, "이영희", "제안팀", "선임연구원",
                    LocalDateTime.now(), null);
        }

        @Test
        @DisplayName("EDITOR 이면 canEdit=true")
        void editorCanEdit() {
            when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireAccess(STEP_ID, USER, ROLE))
                    .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.EDITOR));
            when(fileQueryPort.findBlockFiles(BLOCK_ID, false)).thenReturn(List.of(projection()));

            BlockFileListResult result = service.getBlockFiles(BLOCK_ID, false, USER, ROLE);

            assertThat(result.canEdit()).isTrue();
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).previewable()).isTrue();
        }

        @Test
        @DisplayName("VIEWER 이면 canEdit=false")
        void viewerCannotEdit() {
            when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.of(STEP_ID));
            when(stepAccessUseCase.requireAccess(STEP_ID, USER, ROLE))
                    .thenReturn(new StepAccessUseCase.StepAccessView(STEP_ID, PROJECT_ID, MemberPermission.VIEWER));
            when(fileQueryPort.findBlockFiles(BLOCK_ID, false)).thenReturn(List.of());

            BlockFileListResult result = service.getBlockFiles(BLOCK_ID, false, USER, ROLE);

            assertThat(result.canEdit()).isFalse();
        }

        @Test
        @DisplayName("블록이 없거나 삭제되었으면 FILE_BLOCK_NOT_FOUND")
        void blockNotFound() {
            when(blockCatalogPort.resolveFileBlockStepId(BLOCK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBlockFiles(BLOCK_ID, false, USER, ROLE))
                    .satisfies(hasCode(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("§10 미리보기")
    class Preview {

        @Test
        @DisplayName("완료된 PDF 는 앞 5페이지로 잘라 반환한다")
        void rendersPreview() {
            stubVersionAccess(version(1, UploadStatus.COMPLETED, "pdf"), activeFile(), MemberPermission.VIEWER);
            when(fileStoragePort.getObject(anyString())).thenReturn(new byte[]{1, 2, 3});
            when(pdfPreviewPort.render(any(), any(Integer.class)))
                    .thenReturn(new PdfPreviewPort.Preview(new byte[]{9}, 5, 42));

            FilePreviewResult result = service.getPreview(FILE_VERSION_ID, USER, ROLE);

            assertThat(result.previewPageCount()).isEqualTo(5);
            assertThat(result.totalPageCount()).isEqualTo(42);
        }

        @Test
        @DisplayName("PDF 가 아니면 FILE_PREVIEW_NOT_SUPPORTED")
        void notPreviewable() {
            stubVersionAccess(version(1, UploadStatus.COMPLETED, "docx"), activeFile(), MemberPermission.VIEWER);

            assertThatThrownBy(() -> service.getPreview(FILE_VERSION_ID, USER, ROLE))
                    .satisfies(hasCode(FileErrorCode.FILE_PREVIEW_NOT_SUPPORTED));
            verify(fileStoragePort, never()).getObject(anyString());
        }

        @Test
        @DisplayName("렌더링에 실패하면 FILE_PREVIEW_FAILED")
        void renderFails() {
            stubVersionAccess(version(1, UploadStatus.COMPLETED, "pdf"), activeFile(), MemberPermission.VIEWER);
            when(fileStoragePort.getObject(anyString())).thenReturn(new byte[]{1, 2, 3});
            when(pdfPreviewPort.render(any(), any(Integer.class)))
                    .thenThrow(new RuntimeException("깨진 PDF"));

            assertThatThrownBy(() -> service.getPreview(FILE_VERSION_ID, USER, ROLE))
                    .satisfies(hasCode(FileErrorCode.FILE_PREVIEW_FAILED));
        }
    }
}
