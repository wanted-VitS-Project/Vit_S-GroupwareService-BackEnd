package com.group3.vitamins.file.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
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
import com.group3.vitamins.file.application.usecase.FileCommandUseCase;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
@Slf4j
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
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public FileRenameResult rename(RenameFileCommand command) {
        // 이미 휴지통이거나 없는 문서는 수정 대상이 아니다 (§4 · 404).
        File file = fileRepository.findById(command.fileId())
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));
        requireEditable(command.fileId(), command.requesterUserId(), command.role());

        String before = file.getName();
        String name = validateName(command.name());
        file.rename(name);
        File saved = fileRepository.save(file);

        // 활동 로그(문서명 수정 = MODIFY) — 실제로 이름이 바뀐 경우에만 발행한다 (§파일 rename).
        if (!java.util.Objects.equals(before, saved.getName())) {
            Long blockId = fileQueryPort.findBlockIdByFileId(command.fileId()).orElse(null);
            publishActivity(ActivityLogAction.MODIFY, blockId, saved.getFileId(), saved.getName(),
                    command.requesterUserId(),
                    List.of(new ActivityFieldChange("fileName", before, saved.getName())));
        }

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

        // 링크는 휴지통 이동에 영향받지 않지만, 발행값을 확정하려고 삭제 전에 blockId 를 잡는다.
        Long blockId = fileQueryPort.findBlockIdByFileId(command.fileId()).orElse(null);

        file.moveToTrash(LocalDateTime.now());
        File saved = fileRepository.save(file);

        // 활동 로그(휴지통 이동 = DELETE) — resourceName 은 삭제 전 파일명(이동해도 name 은 그대로) (§파일 trash).
        publishActivity(ActivityLogAction.DELETE, blockId, saved.getFileId(), saved.getName(),
                command.requesterUserId(),
                List.of(new ActivityFieldChange(null, null, null)));

        return new FileTrashResult(saved.getFileId(), saved.getDeletedAt());
    }

    @Override
    public FileRestoreResult restore(RestoreFileCommand command) {
        File file = fileRepository.findById(command.fileId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_NOT_FOUND));

        // ⭐ 블록이 삭제됐어도 복구된다(§6) — 삭제된 블록의 스텝으로도 권한을 판정한다.
        Long stepId = fileQueryPort.findStepIdByFileIdIncludingDeletedBlock(command.fileId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_BLOCK_NOT_FOUND));
        requireEditableOnStep(stepId, command.requesterUserId(), command.role());

        // 휴지통에 있는 문서만 복구 대상이다 (§6 · 400).
        if (!file.isDeleted()) {
            throw new ValidationException(FileErrorCode.FILE_NOT_DELETED);
        }

        file.restoreFromTrash();
        File saved = fileRepository.save(file);

        // 원래 블록이 살아있으면 그 블록으로, soft delete 됐으면 blockId=null·blockDeleted=true (§6).
        Long linkedBlockId = fileQueryPort.findBlockIdByFileId(command.fileId()).orElse(null);
        boolean blockAlive = linkedBlockId != null
                && blockCatalogPort.resolveAttachableBlockStepId(linkedBlockId).isPresent();

        // 활동 로그(복원 = RESTORE) — 블록이 soft delete 됐어도 link 행은 남아 원래 blockId 로 발행한다.
        // 블록 활동 스트림에 함께 노출하는 게 컨벤션이라(별도 휴지통 화면 없음) 살아있음 여부와 무관하게 원래 블록에 건다.
        publishActivity(ActivityLogAction.RESTORE, linkedBlockId, saved.getFileId(), saved.getName(),
                command.requesterUserId(),
                List.of(new ActivityFieldChange(null, null, null)));

        return new FileRestoreResult(
                saved.getFileId(), saved.getName(),
                blockAlive ? linkedBlockId : null, !blockAlive);
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

        // ⚠️ 발행값은 삭제 前에 캡처한다 — block_file 이 fk_block_file_file 로 ON DELETE CASCADE 라
        //    file 을 지운 뒤 findBlockIdByFileId 하면 링크가 이미 사라져 empty 가 된다(PURGE 로그 유실).
        String purgedName = file.getName();
        Long purgedBlockId = fileQueryPort.findBlockIdByFileId(command.fileId()).orElse(null);

        // 저장소 키 수집(UPLOADING/FAILED 포함 — 미완료 버전도 객체가 있을 수 있다).
        List<FileVersion> versions = fileVersionRepository.findByFileId(command.fileId());
        List<String> storageKeys = versions.stream().map(FileVersion::getStorageKey).toList();

        // ⭐ DB 삭제 前 파생데이터 정리 — 타 도메인(비타메이트)이 file_version 참조를 먼저 끊는다.
        //    파일 도메인은 상대 내부 테이블을 모른 채 포트만 호출한다(구현체는 비타메이트 소유, 같은 트랜잭션).
        fileDerivedDataCleanupPort.cleanupByFileId(command.fileId());

        // file_version 을 먼저 지운다(fk_file_version_file 에 CASCADE 없음), 그다음 file(block_file 은 CASCADE).
        fileVersionRepository.deleteByFileId(command.fileId());
        fileRepository.deleteById(command.fileId());

        // 저장소 객체 삭제는 DB 커밋 후에 실행한다 — 커밋이 실패하면 S3 를 건드리지 않아
        // "S3 는 지웠는데 DB 는 롤백" 유실을 막는다(§7). 커밋 뒤 S3 실패는 허용(DB 는 지워졌고 남은 키는 정리 대상).
        runAfterCommit(() -> fileStoragePort.deleteObjects(storageKeys));

        // 활동 로그(영구 삭제 = PURGE) — 삭제 전에 잡아둔 blockId·파일명으로 발행한다.
        publishActivity(ActivityLogAction.PURGE, purgedBlockId, command.fileId(), purgedName,
                command.requesterUserId(),
                List.of(new ActivityFieldChange(null, null, null)));

        // storageDeletedCount = 삭제를 요청한 객체 수. 실제 삭제는 커밋 후라 응답 시점엔 알 수 없다.
        return new FilePermanentDeleteResult(command.fileId(), versions.size(), storageKeys.size());
    }

    /**
     * 활동 로그를 발행하되, 붙일 블록이 없으면(링크 유실된 고아 파일) 건너뛴다.
     * ActivityOccurredEvent 는 blockId 가 null 이면 예외를 던지므로, 로그 하나 때문에
     * 파일 작업(삭제·복원 등) 자체가 실패하지 않도록 여기서 방어한다.
     */
    private void publishActivity(ActivityLogAction action, Long blockId, Long resourceId,
                                 String resourceName, String actorId, List<ActivityFieldChange> changes) {
        if (blockId == null) {
            log.warn("활동 로그 건너뜀(블록 링크 없음) - action={}, fileId={}", action, resourceId);
            return;
        }
        domainEventPublisher.publish(
                ActivityOccurredEvent.of(action, blockId, resourceId, resourceName, actorId, changes));
    }

    /** 트랜잭션이 있으면 커밋 성공 후 실행하고, 없으면(예: 단위 테스트) 즉시 실행한다. */
    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
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
        requireEditableOnStep(stepId, userId, role);
    }

    /** 스텝 EDITOR 판정 + 파일 계약 코드 변환. 스텝 ID 를 이미 아는 호출자용(§6 복구는 삭제 블록의 스텝을 넘긴다). */
    private void requireEditableOnStep(Long stepId, String userId, String role) {
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
