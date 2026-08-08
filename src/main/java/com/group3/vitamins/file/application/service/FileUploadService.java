package com.group3.vitamins.file.application.service;

import com.group3.vitamins.file.application.command.CompleteFileUploadCommand;
import com.group3.vitamins.file.application.command.StartFileUploadCommand;
import com.group3.vitamins.file.application.port.*;
import com.group3.vitamins.file.application.result.FileUploadStartResult;
import com.group3.vitamins.file.application.result.FileVersionDetailResult;
import com.group3.vitamins.file.application.usecase.FileUploadUseCase;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.repository.BlockFileRepository;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 파일 업로드 서비스 (§1 시작). 파일은 블록에 붙지만 스텝 권한을 따르므로
 * {@code BlockCatalogPort}(blockId→stepId) + {@code StepAccessUseCase}(EDITOR 판정)를 조합한다.
 *
 * <p>새 문서 vs 새 버전을 한 API 가 처리한다 — {@code fileId} 유무로 갈린다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FileUploadService implements FileUploadUseCase {

    /** 50MB (FILE-007). */
    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;

    /** 실행 파일 블랙리스트 (FILE-008). 나머지는 허용. */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "sh", "jar", "cmd", "com", "msi", "scr", "dll", "bin", "app");

    private final BlockCatalogPort blockCatalogPort;
    private final StepAccessUseCase stepAccessUseCase;
    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final BlockFileRepository blockFileRepository;
    private final FileQueryPort fileQueryPort;
    private final UploaderLookupPort uploaderLookupPort;
    private final FileStoragePort fileStoragePort;
    private final PdfPageCounterPort pdfPageCounterPort;
    private final FileVersionFailureRecorder failureRecorder;
    private final FileIndexTriggerPort fileIndexTriggerPort;

    @Override
    public FileUploadStartResult startUpload(StartFileUploadCommand command) {
        Long stepId = blockCatalogPort.resolveAttachableBlockStepId(command.blockId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));

        Long projectId = requireEditable(stepId, command.requesterUserId(), command.role()).projectId();

        validateInput(command);
        String extension = extractExtension(command.originalFileName());

        long fileId;
        int versionNo;
        boolean newDocument = command.fileId() == null;

        if (newDocument) {
            String docName = resolveDocumentName(command.name(), command.originalFileName());
            if (!command.allowDuplicateName()
                    && fileQueryPort.existsActiveNameInBlock(command.blockId(), docName)) {
                throw new ConflictException(FileErrorCode.FILE_NAME_DUPLICATED);
            }
            File saved = fileRepository.save(File.create(projectId, docName, command.requesterUserId()));
            fileId = saved.getFileId();
            versionNo = 1;
        } else {
            File file = fileRepository.findById(command.fileId())
                    .filter(f -> !f.isDeleted())
                    .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));
            fileId = file.getFileId();
            versionNo = fileVersionRepository.findMaxVersionNo(fileId) + 1;
        }

        UploaderLookupPort.UploaderSnapshot uploader =
                uploaderLookupPort.findByUserId(command.requesterUserId())
                        .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_INVALID_REQUEST));

        String storageKey = buildStorageKey(projectId, fileId, versionNo, extension);

        FileVersion version = fileVersionRepository.save(FileVersion.startUpload(
                fileId, versionNo, storageKey,
                command.originalFileName(), extension, command.mimeType(), command.sizeBytes(),
                command.comment(),
                command.requesterUserId(), uploader.name(), uploader.department(), uploader.position()));

        if (newDocument) {
            blockFileRepository.link(command.blockId(), fileId, command.requesterUserId());
        }

        FileStoragePort.PresignedUrl presigned =
                fileStoragePort.presignUpload(storageKey, command.mimeType(), command.sizeBytes());

        return new FileUploadStartResult(
                fileId, version.getFileVersionId(), versionNo, presigned.url(), presigned.expiresAt());
    }

    @Override
    public FileVersionDetailResult completeUpload(CompleteFileUploadCommand command) {
        FileVersion version = fileVersionRepository.findById(command.fileVersionId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_VERSION_NOT_FOUND));

        if (version.isCompleted()) {
            throw new ValidationException(FileErrorCode.FILE_ALREADY_COMPLETED);
        }

        Long blockId = fileQueryPort.findBlockIdByFileId(version.getFileId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        Long stepId = blockCatalogPort.resolveAttachableBlockStepId(blockId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        requireEditable(stepId, command.requesterUserId(), command.role());

        // 실패 전이는 별도 트랜잭션(REQUIRES_NEW)으로 확정 저장한다 — 여기서 예외를 던지면
        // 이 서비스의 @Transactional 이 롤백되므로 인라인 save 는 사라진다. 객체 없음·크기 불일치 모두 대칭 처리.
        FileStoragePort.StoredObject stored = fileStoragePort.head(version.getStorageKey())
                .orElse(null);
        if (stored == null) {
            failureRecorder.markFailed(version);
            throw new ConflictException(FileErrorCode.FILE_OBJECT_NOT_FOUND);
        }
        if (stored.sizeBytes() != version.getSizeBytes()) {
            failureRecorder.markFailed(version);
            throw new ConflictException(FileErrorCode.FILE_SIZE_MISMATCH);
        }

        Integer pageCount = null;
        if (version.isPreviewable()) {
            pageCount = pdfPageCounterPort
                    .countPages(fileStoragePort.getObject(version.getStorageKey()))
                    .orElse(null);
        }

        version.complete(stored.sizeBytes(), command.checksum(), pageCount, LocalDateTime.now());
        FileVersion saved = fileVersionRepository.save(version);
        fileIndexTriggerPort.triggerIndexing(saved.getFileVersionId());

        File file = fileRepository.findById(saved.getFileId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));

        return toDetail(file, saved);
    }

    private FileVersionDetailResult toDetail(File file, FileVersion v) {
        return new FileVersionDetailResult(
                file.getFileId(), v.getFileVersionId(), v.getVersionNo(), file.getName(),
                v.getOriginalFileName(), v.getExtension(), v.getSizeBytes(), v.getPageCount(),
                v.getComment(), v.getUploaderName(), v.getUploaderDepartment(), v.getUploaderPosition(),
                v.getCompletedAt());
    }

    /** 스텝 편집 권한을 확인하고 판정 결과를 돌려준다. 권한 실패는 파일 계약 코드로 변환한다. */
    private StepAccessUseCase.StepAccessView requireEditable(Long stepId, String userId, String role) {
        try {
            return stepAccessUseCase.requireEditable(stepId, userId, role);
        } catch (ForbiddenException | NotFoundException e) {
            throw new ForbiddenException(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED, e);
        }
    }

    private void validateInput(StartFileUploadCommand command) {
        if (command.originalFileName() == null || command.originalFileName().isBlank()) {
            throw new ValidationException(FileErrorCode.FILE_INVALID_REQUEST);
        }
        if (command.sizeBytes() <= 0) {
            throw new ValidationException(FileErrorCode.FILE_INVALID_REQUEST);
        }
        if (command.sizeBytes() > MAX_SIZE_BYTES) {
            throw new ValidationException(FileErrorCode.FILE_SIZE_EXCEEDED);
        }
        if (BLOCKED_EXTENSIONS.contains(extractExtension(command.originalFileName()))) {
            throw new ValidationException(FileErrorCode.FILE_EXTENSION_BLOCKED);
        }
    }

    /** 확장자(소문자, 점 제외). 없으면 빈 문자열. */
    private String extractExtension(String originalFileName) {
        int dot = originalFileName.lastIndexOf('.');
        if (dot < 0 || dot == originalFileName.length() - 1) {
            return "";
        }
        return originalFileName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }

    /** 표시명 — 명시 name 이 있으면 그대로, 없으면 원본 파일명에서 확장자를 뗀 값(§1). */
    private String resolveDocumentName(String name, String originalFileName) {
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        int dot = originalFileName.lastIndexOf('.');
        return dot > 0 ? originalFileName.substring(0, dot) : originalFileName;
    }

    /**
     * 저장 키: {@code projects/{projectId}/files/{fileId}/versions/{versionNo}/{uuid}[.ext]}.
     * ⚠️ 경로에 fileVersionId 대신 versionNo 를 쓴다 — fileVersionId 는 INSERT 전에 알 수 없고
     * storageKey 는 버전 생성 시 확정돼야 하며, uuid 가 유일성을 보장하므로 안전하다(2026-08-06).
     */
    private String buildStorageKey(long projectId, long fileId, int versionNo, String extension) {
        String uuid = UUID.randomUUID().toString();
        String suffix = extension.isEmpty() ? "" : "." + extension;
        return "projects/%d/files/%d/versions/%d/%s%s".formatted(projectId, fileId, versionNo, uuid, suffix);
    }
}
