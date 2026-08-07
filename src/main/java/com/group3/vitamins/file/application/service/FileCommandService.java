package com.group3.vitamins.file.application.service;

import com.group3.vitamins.file.application.command.PermanentDeleteFileCommand;
import com.group3.vitamins.file.application.command.RenameFileCommand;
import com.group3.vitamins.file.application.command.TrashFileCommand;
import com.group3.vitamins.file.application.port.ApprovalLockQueryPort;
import com.group3.vitamins.file.application.port.BlockCatalogPort;
import com.group3.vitamins.file.application.port.FileDerivedDataCleanupPort;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.result.FilePermanentDeleteResult;
import com.group3.vitamins.file.application.result.FileRenameResult;
import com.group3.vitamins.file.application.result.FileTrashResult;
import com.group3.vitamins.file.application.usecase.FileCommandUseCase;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
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
import java.util.List;

/**
 * 파일(문서) 쓰기 서비스 (#135 · §4 문서명 수정 · §5 휴지통 이동 · §7 영구삭제).
 *
 * <p>스텝 EDITOR 권한을 따르며, 권한 실패는 파일 계약 코드({@code FILE_EDIT_PERMISSION_REQUIRED})로 변환한다.
 * 판정 순서는 <b>존재(404) → 권한(403) → 상태·검증(400) → 결재 잠금(409)</b> 이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FileCommandService implements FileCommandUseCase {

    /** 문서명 최대 길이(§4). */
    private static final int MAX_NAME_LENGTH = 255;

    /** 영구 삭제 확인 문자(§7 · FILE-023). 정확히 일치해야 한다. */
    private static final String PERMANENT_DELETE_CONFIRM_TEXT = "영구 삭제";

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final FileQueryPort fileQueryPort;
    private final BlockCatalogPort blockCatalogPort;
    private final StepAccessUseCase stepAccessUseCase;
    private final ApprovalLockQueryPort approvalLockQueryPort;
    private final FileStoragePort fileStoragePort;
    private final FileDerivedDataCleanupPort fileDerivedDataCleanupPort;

    @Override
    public FileRenameResult rename(RenameFileCommand command) {
        // 이미 휴지통이거나 없는 문서는 수정 대상이 아니다 (§4 · 404).
        File file = fileRepository.findById(command.fileId())
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));
        requireEditable(command.fileId(), command.requesterUserId(), command.role());

        String name = validateName(command.name());
        file.rename(name);
        File saved = fileRepository.save(file);

        return new FileRenameResult(saved.getFileId(), saved.getName());
    }

    @Override
    public FileTrashResult moveToTrash(TrashFileCommand command) {
        File file = fileRepository.findById(command.fileId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));
        requireEditable(command.fileId(), command.requesterUserId(), command.role());

        if (file.isDeleted()) {
            throw new ValidationException(FileErrorCode.FILE_ALREADY_DELETED);
        }

        // 진행 중 결재의 대상이면 삭제 불가 — message 에 결재 정보를 담는다 (§5 · 409).
        approvalLockQueryPort.findInProgressApproval(command.fileId())
                .ifPresent(approval -> {
                    throw new ConflictException(
                            FileErrorCode.FILE_APPROVAL_IN_PROGRESS,
                            "진행 중인 결재의 대상 문서입니다 (결재: %s). 결재를 회수하거나 완료한 뒤 삭제하세요."
                                    .formatted(approval.title()));
                });

        file.moveToTrash(LocalDateTime.now());
        File saved = fileRepository.save(file);

        return new FileTrashResult(saved.getFileId(), saved.getDeletedAt());
    }

    @Override
    public FilePermanentDeleteResult permanentDelete(PermanentDeleteFileCommand command) {
        File file = fileRepository.findById(command.fileId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));
        requireEditable(command.fileId(), command.requesterUserId(), command.role());

        // 휴지통에 있는 문서만 영구 삭제할 수 있다 (§7 · 400).
        if (!file.isDeleted()) {
            throw new ValidationException(FileErrorCode.FILE_NOT_DELETED);
        }
        // 확인 문자를 서버가 검증한다 — 정확히 "영구 삭제" 여야 한다 (§7 · 400 · FILE-023).
        if (!PERMANENT_DELETE_CONFIRM_TEXT.equals(command.confirmText())) {
            throw new ValidationException(FileErrorCode.FILE_CONFIRM_TEXT_MISMATCH);
        }
        // 완료 결재까지 포함해 이 파일의 버전을 참조하는 결재가 있으면 막는다 (§7 · 409).
        if (approvalLockQueryPort.existsAnyApprovalReference(command.fileId())) {
            throw new ConflictException(FileErrorCode.FILE_APPROVAL_REFERENCED);
        }

        // 저장소 키 수집(UPLOADING/FAILED 포함 — 미완료 버전도 객체가 있을 수 있다).
        List<FileVersion> versions = fileVersionRepository.findByFileId(command.fileId());
        List<String> storageKeys = versions.stream().map(FileVersion::getStorageKey).toList();

        // ⭐ DB 삭제 前 파생데이터 정리 — 타 도메인(비타메이트)이 file_version 참조를 먼저 끊는다.
        //    파일 도메인은 상대 내부 테이블을 모른 채 포트만 호출한다(구현체는 비타메이트 소유, 같은 트랜잭션).
        fileDerivedDataCleanupPort.cleanupByFileId(command.fileId());

        // file_version 을 먼저 지운다(fk_file_version_file 에 CASCADE 없음), 그다음 file(block_file 은 CASCADE).
        fileVersionRepository.deleteByFileId(command.fileId());
        fileRepository.deleteById(command.fileId());

        // 저장소 객체 삭제 — 일부 실패해도 DB 삭제는 유지한다(§7). 실패 키는 정리 대상.
        int storageDeleted = fileStoragePort.deleteObjects(storageKeys);

        return new FilePermanentDeleteResult(command.fileId(), versions.size(), storageDeleted);
    }

    private String validateName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new ValidationException(FileErrorCode.FILE_INVALID_REQUEST);
        }
        String name = rawName.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new ValidationException(FileErrorCode.FILE_INVALID_REQUEST);
        }
        return name;
    }

    /** 문서 → 블록 → 스텝 경로로 편집 권한(EDITOR)을 확인한다. 권한 실패는 파일 계약 코드로 변환. */
    private void requireEditable(Long fileId, String userId, String role) {
        Long blockId = fileQueryPort.findBlockIdByFileId(fileId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        // 결재 블록에 매달린 파일도 이름수정·휴지통 이동이 되어야 하므로 attachable(FILE|APPROVAL) 로 해석한다.
        Long stepId = blockCatalogPort.resolveAttachableBlockStepId(blockId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        try {
            stepAccessUseCase.requireEditable(stepId, userId, role);
        } catch (ForbiddenException e) {
            // 권한 부족만 403 으로 변환한다.
            throw new ForbiddenException(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED, e);
        } catch (NotFoundException e) {
            // 스텝 자체가 없는 건 참조 유실(정합성 문제)이므로 권한 오류로 위장하지 않고 404 로 남긴다.
            throw new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND, e);
        }
    }
}
