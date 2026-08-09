package com.group3.vitamins.file.application.service;

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
import com.group3.vitamins.file.application.result.ProjectFileProjection;
import com.group3.vitamins.file.application.result.ProjectFileResult;
import com.group3.vitamins.file.application.result.ProjectFileVersionProjection;
import com.group3.vitamins.file.application.result.ProjectFileVersionResult;
import com.group3.vitamins.file.application.result.VersionHistoryResult;
import com.group3.vitamins.file.application.usecase.FileQueryUseCase;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.exception.FilePreviewException;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 파일 조회 서비스 (#134 조회 5종 + #138 버전목록). 읽기 전용이며 §1~§5·§8~§11 은 스텝 접근 권한을,
 * 버전 목록(§11, #138)은 프로젝트 접근 권한을 따른다(둘 다 VIEWER 이상).
 * 권한 실패는 파일 계약 코드({@code FILE_ACCESS_PERMISSION_REQUIRED})로 변환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileQueryService implements FileQueryUseCase {

    private final FileVersionRepository fileVersionRepository;
    private final FileRepository fileRepository;
    private final FileQueryPort fileQueryPort;
    private final BlockCatalogPort blockCatalogPort;
    private final StepAccessUseCase stepAccessUseCase;
    private final FileStoragePort fileStoragePort;
    private final PdfPreviewPort pdfPreviewPort;
    private final ProjectAccessUseCase projectAccessUseCase;

    /** 미리보기 최대 페이지 수 (§10). */
    private static final int MAX_PREVIEW_PAGES = 5;

    @Override
    public DownloadUrlResult getDownloadUrl(Long fileVersionId, String requesterUserId, String role) {
        VersionContext ctx = requireVersionAccess(fileVersionId, requesterUserId, role);

        // 문서가 휴지통이면 다운로드 불가 — 버전 없음과 동일하게 404 (§9).
        if (ctx.file().isDeleted()) {
            throw new NotFoundException(FileErrorCode.FILE_VERSION_NOT_FOUND);
        }
        if (!ctx.version().isCompleted()) {
            throw new ConflictException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }

        FileStoragePort.PresignedUrl presigned = fileStoragePort.presignDownload(
                ctx.version().getStorageKey(), ctx.version().getOriginalFileName());

        return new DownloadUrlResult(
                ctx.version().getFileVersionId(), ctx.version().getOriginalFileName(),
                ctx.version().getSizeBytes(), presigned.url(), presigned.expiresAt());
    }

    @Override
    public FileVersionSingleResult getVersion(Long fileVersionId, String requesterUserId, String role) {
        VersionContext ctx = requireVersionAccess(fileVersionId, requesterUserId, role);
        FileVersion v = ctx.version();
        // §8 이력과 동일 정의 — 완료(COMPLETED) 버전 기준 최대 차수. findMaxVersionNo 는 UPLOADING·FAILED 도
        // 세므로, 새 버전 업로드 시작 직후 §11 이 §8 과 latest 판정이 어긋나는 문제를 막는다.
        int latestVersionNo = fileQueryPort.findMaxCompletedVersionNo(v.getFileId());

        return new FileVersionSingleResult(
                v.getFileVersionId(), v.getFileId(), ctx.file().getName(), v.getVersionNo(),
                v.getVersionNo() == latestVersionNo, latestVersionNo,
                v.getOriginalFileName(), v.getExtension(), v.getSizeBytes(), v.getPageCount(),
                v.isPreviewable(), v.getComment(), v.getUploaderName(), v.getUploaderDepartment(),
                v.getUploaderPosition(), v.getCompletedAt(), ctx.file().isDeleted());
    }

    @Override
    public VersionHistoryResult getVersionHistory(Long fileId, String requesterUserId, String role) {
        File file = fileRepository.findById(fileId)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));
        requireStepAccess(resolveStepId(fileId), requesterUserId, role);

        List<FileVersionProjection> versions = fileQueryPort.findCompletedVersions(fileId);
        // 차수 내림차순이라 첫 행이 최신 차수. 비어 있으면 0.
        int latestVersionNo = versions.isEmpty() ? 0 : versions.get(0).versionNo();

        List<VersionHistoryResult.Item> items = versions.stream()
                .map(p -> new VersionHistoryResult.Item(
                        p.fileVersionId(), p.versionNo(), p.versionNo() == latestVersionNo,
                        p.originalFileName(), p.extension(), p.sizeBytes(), p.pageCount(),
                        isPreviewable(p.extension()), p.comment(),
                        p.uploaderName(), p.uploaderDepartment(), p.uploaderPosition(), p.completedAt()))
                .toList();

        return new VersionHistoryResult(fileId, file.getName(), items.size(), items);
    }

    @Override
    public BlockFileListResult getBlockFiles(Long blockId, boolean deleted, String requesterUserId, String role) {
        Long stepId = blockCatalogPort.resolveFileBlockStepId(blockId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        StepAccessUseCase.StepAccessView view = requireStepAccess(stepId, requesterUserId, role);
        boolean canEdit = view.permission() == MemberPermission.EDITOR;

        List<BlockFileListResult.Item> items = fileQueryPort.findBlockFiles(blockId, deleted).stream()
                .map(p -> new BlockFileListResult.Item(
                        p.fileId(), p.name(), p.latestVersionId(), p.latestVersionNo(), p.versionCount(),
                        p.originalFileName(), p.extension(), p.sizeBytes(), isPreviewable(p.extension()),
                        p.uploaderName(), p.uploaderDepartment(), p.uploaderPosition(), p.updatedAt(), p.deletedAt()))
                .toList();

        return new BlockFileListResult(blockId, canEdit, items);
    }

    @Override
    public FilePreviewResult getPreview(Long fileVersionId, String requesterUserId, String role) {
        VersionContext ctx = requireVersionAccess(fileVersionId, requesterUserId, role);

        if (ctx.file().isDeleted()) {
            throw new NotFoundException(FileErrorCode.FILE_VERSION_NOT_FOUND);
        }
        if (!ctx.version().isCompleted()) {
            throw new ConflictException(FileErrorCode.FILE_UPLOAD_NOT_COMPLETED);
        }
        if (!ctx.version().isPreviewable()) {
            throw new ConflictException(FileErrorCode.FILE_PREVIEW_NOT_SUPPORTED);
        }

        // getObject 도 try 안에 둔다 — S3 조회 실패(SdkException 등)도 FILE_PREVIEW_FAILED 로 변환해
        // 원시 런타임 예외가 500 계열로 새는 것을 막는다.
        try {
            byte[] bytes = fileStoragePort.getObject(ctx.version().getStorageKey());
            PdfPreviewPort.Preview preview = pdfPreviewPort.render(bytes, MAX_PREVIEW_PAGES);
            return new FilePreviewResult(
                    preview.content(), preview.previewPageCount(), preview.totalPageCount());
        } catch (RuntimeException e) {
            throw new FilePreviewException(FileErrorCode.FILE_PREVIEW_FAILED, e);
        }
    }

    @Override
    public List<ProjectFileVersionResult> getProjectFileVersions(Long projectId, String requesterUserId, String role) {
        requireProjectAccess(projectId, requesterUserId, role);

        List<ProjectFileVersionProjection> rows = fileQueryPort.findProjectFileVersions(projectId);
        // 파일별 최대 완료 차수 — latest 판정용. 정렬에 의존하지 않고 집계로 구한다.
        Map<Long, Integer> maxVersionNoByFile = rows.stream()
                .collect(Collectors.toMap(
                        ProjectFileVersionProjection::fileId,
                        ProjectFileVersionProjection::versionNo,
                        Integer::max));

        return rows.stream()
                .map(p -> new ProjectFileVersionResult(
                        p.fileId(), p.name(), p.fileVersionId(), p.versionNo(),
                        p.versionNo() == maxVersionNoByFile.get(p.fileId()),
                        p.originalFileName(), p.extension(), p.sizeBytes(), p.pageCount(),
                        isPreviewable(p.extension()), p.completedAt(), p.indexStatus()))
                .toList();
    }

    @Override
    public List<ProjectFileResult> getProjectFiles(Long projectId, String requesterUserId, String role) {
        requireProjectAccess(projectId, requesterUserId, role);

        return fileQueryPort.findProjectFiles(projectId).stream()
                .map(p -> new ProjectFileResult(
                        p.stepId(), p.stepName(), p.blockId(), p.blockTitle(), p.blockDeleted(),
                        p.fileId(), p.name(), p.latestVersionId(), p.latestVersionNo(), p.versionCount(),
                        p.originalFileName(), p.extension(), p.sizeBytes(), isPreviewable(p.extension()),
                        p.uploaderName(), p.uploaderDepartment(), p.uploaderPosition(), p.updatedAt()))
                .toList();
    }

    private boolean isPreviewable(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    /**
     * 버전 → 문서 → 블록 → 스텝 경로로 접근 권한을 확인하고 (version, file) 을 돌려준다.
     * ⛔ 여기서는 휴지통 여부로 거르지 않는다 — §11(버전 단건)은 휴지통도 반환해야 하므로 호출자가 판단한다.
     */
    private VersionContext requireVersionAccess(Long fileVersionId, String userId, String role) {
        FileVersion version = fileVersionRepository.findById(fileVersionId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_VERSION_NOT_FOUND));
        File file = fileRepository.findById(version.getFileId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_VERSION_NOT_FOUND));
        Long stepId = resolveStepId(file.getFileId());
        requireStepAccess(stepId, userId, role);
        return new VersionContext(version, file);
    }

    private Long resolveStepId(Long fileId) {
        Long blockId = fileQueryPort.findBlockIdByFileId(fileId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        // 결재 블록에 매달린 파일도 버전/다운로드/미리보기가 되어야 하므로 attachable(FILE|APPROVAL) 로 해석한다.
        return blockCatalogPort.resolveAttachableBlockStepId(blockId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
    }

    /** 스텝 열람 권한(VIEWER 이상) 확인 후 판정 결과를 돌려준다(canEdit 계산용). 실패는 파일 계약 코드로 변환. */
    private StepAccessUseCase.StepAccessView requireStepAccess(Long stepId, String userId, String role) {
        try {
            return stepAccessUseCase.requireAccess(stepId, userId, role);
        } catch (ForbiddenException | NotFoundException e) {
            throw new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED, e);
        }
    }

    /**
     * 프로젝트 열람 권한(VIEWER 이상)을 확인한다(§11, #138). 권한 없음(403)만 파일 계약 코드로 변환하고,
     * 프로젝트 없음(404 {@code PROJECT_NOT_FOUND})은 그대로 통과시킨다 — file.md 계약이 두 코드를 다르게 요구한다.
     */
    private void requireProjectAccess(Long projectId, String userId, String role) {
        try {
            projectAccessUseCase.requireAccess(projectId, userId, role);
        } catch (ForbiddenException e) {
            throw new ForbiddenException(FileErrorCode.FILE_ACCESS_PERMISSION_REQUIRED, e);
        }
    }

    private record VersionContext(FileVersion version, File file) {
    }
}
