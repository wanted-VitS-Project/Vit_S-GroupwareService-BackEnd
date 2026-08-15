package com.group3.vitamins.file.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.file.application.command.CompleteFileUploadCommand;
import com.group3.vitamins.file.application.command.StartFileUploadCommand;
import com.group3.vitamins.file.application.port.*;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.file.application.result.FileUploadStartResult;
import com.group3.vitamins.file.application.result.FileVersionDetailResult;
import com.group3.vitamins.file.application.usecase.FileUploadUseCase;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.repository.BlockFileRepository;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final StorageKeyBuilder storageKeyBuilder;

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
            // 결재 블록은 동명 검사를 면제한다(§1, 2026-08-16). 결재 문서 제거는 링크만 끊고 파일은
            // 블록에 남는데 그 파일은 §3 목록에서 빠져 사용자가 지울 수 없다 — 검사를 두면 "제거 후
            // 같은 파일 재첨부"가 영구히 409 다. FILE 블록은 종전대로 되묻는다.
            if (!command.allowDuplicateName()
                    && !blockCatalogPort.isApprovalBlock(command.blockId())
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
            // command.fileId() 는 클라이언트가 지정한다 — 권한을 검증한 블록(command.blockId())이 실제로 이
            // 파일의 소유 블록인지 확인하지 않으면 타 블록·타 프로젝트·타 회사 문서에 버전을 붙일 수 있다(IDOR).
            // completeUpload 와 같은 방식으로 파일의 소유 블록을 재조회해 일치를 강제한다. 불일치는 존재를
            // 숨겨 FILE_NOT_FOUND 로 통일한다(타 회사 문서 존재 열거 차단).
            Long ownerBlockId = fileQueryPort.findBlockIdByFileId(file.getFileId())
                    .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));
            if (!ownerBlockId.equals(command.blockId())) {
                throw new NotFoundException(FileErrorCode.FILE_NOT_FOUND);
            }
            fileId = file.getFileId();
            versionNo = fileVersionRepository.findMaxVersionNo(fileId) + 1;
        }

        UploaderLookupPort.UploaderSnapshot uploader =
                uploaderLookupPort.findByUserId(command.requesterUserId())
                        .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_INVALID_REQUEST));

        String storageKey = storageKeyBuilder.build(
                currentCompanyIdProvider.currentCompanyId(), projectId, fileId, versionNo, extension);

        FileVersion version = fileVersionRepository.save(FileVersion.startUpload(
                fileId, versionNo, storageKey,
                command.originalFileName(), extension, command.mimeType(), command.sizeBytes(),
                command.comment(),
                command.requesterUserId(), uploader.name(), uploader.department(), uploader.position(),
                null));

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

        // 활동 로그(업로드 완료 = CREATE) — 새 문서 첫 버전이든 기존 문서 새 버전이든 완료 시점에 발행한다
        // (§파일 upload). blockId 는 위에서 resolveAttachableBlockStepId 판정에 쓴 그 링크로 non-null 이 보장된다.
        domainEventPublisher.publish(ActivityOccurredEvent.of(
                ActivityLogAction.CREATE,
                blockId,
                file.getFileId(),
                file.getName(),
                command.requesterUserId(),
                List.of(new ActivityFieldChange(null, null, null))
        ));

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
}
