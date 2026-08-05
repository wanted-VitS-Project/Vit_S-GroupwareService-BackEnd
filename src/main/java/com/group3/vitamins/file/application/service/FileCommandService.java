package com.group3.vitamins.file.application.service;

import com.group3.vitamins.file.application.command.RenameFileCommand;
import com.group3.vitamins.file.application.command.TrashFileCommand;
import com.group3.vitamins.file.application.port.ApprovalLockQueryPort;
import com.group3.vitamins.file.application.port.BlockCatalogPort;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.result.FileRenameResult;
import com.group3.vitamins.file.application.result.FileTrashResult;
import com.group3.vitamins.file.application.usecase.FileCommandUseCase;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 파일(문서) 쓰기 서비스 (#135 · §4 문서명 수정 · §5 휴지통 이동).
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

    private final FileRepository fileRepository;
    private final FileQueryPort fileQueryPort;
    private final BlockCatalogPort blockCatalogPort;
    private final StepAccessUseCase stepAccessUseCase;
    private final ApprovalLockQueryPort approvalLockQueryPort;

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
        Long stepId = blockCatalogPort.resolveFileBlockStepId(blockId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        try {
            stepAccessUseCase.requireEditable(stepId, userId, role);
        } catch (ForbiddenException | NotFoundException e) {
            throw new ForbiddenException(FileErrorCode.FILE_EDIT_PERMISSION_REQUIRED);
        }
    }
}
