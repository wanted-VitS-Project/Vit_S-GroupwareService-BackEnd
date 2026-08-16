package com.group3.vitamins.checklist.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.checklist.application.command.CreateChecklistItemCommand;
import com.group3.vitamins.checklist.application.command.DeleteChecklistItemCommand;
import com.group3.vitamins.checklist.application.command.UpdateChecklistItemCommand;
import com.group3.vitamins.checklist.application.policy.ChecklistEligibilityPolicy;
import com.group3.vitamins.checklist.application.usecase.ChecklistCommandUseCase;
import com.group3.vitamins.checklist.domain.exception.ChecklistErrorCode;
import com.group3.vitamins.checklist.domain.model.ChecklistItem;
import com.group3.vitamins.checklist.domain.repository.ChecklistBlockRepository;
import com.group3.vitamins.checklist.domain.repository.ChecklistRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 체크리스트 블록 생성·삭제는 Block 도메인(동훈님)이 처리한다 — 여기는 항목(내부 데이터) 생성·수정·삭제만 담당한다.
 * 블록 삭제 시 소속 항목 정리는 {@link ChecklistHandlerService} 가 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChecklistCommandService implements ChecklistCommandUseCase {

    private final ChecklistEligibilityPolicy eligibilityPolicy;
    private final ChecklistRepository checklistRepository;
    private final ChecklistBlockRepository checklistBlockRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public CreateChecklistItemView create(CreateChecklistItemCommand command) {
        log.info("체크리스트 항목 생성 요청 - chkBlockId={}, userId={}", command.chkBlockId(), command.userId());

        if (command.content() == null || command.content().isBlank()) {
            throw new ValidationException(ChecklistErrorCode.INVALID_CONTENT);
        }

        eligibilityPolicy.assertBlockActiveOrThrow(command.chkBlockId());
        eligibilityPolicy.assertEditPermission(command.chkBlockId(), command.userId(), command.role());

        ChecklistItem created = checklistRepository.create(command.chkBlockId(), command.content());
        ChecklistRepository.CountSummary summary = checklistRepository.countSummary(command.chkBlockId());
        int completedCount = summary.completedCount();
        int totalCount = summary.totalCount();

        log.info("체크리스트 항목 생성 완료 - chkId={}", created.getChkId());

        // 활동 로그(항목 생성, §5.2) — resourceName 에 항목 내용을 스냅샷으로 남긴다.
        Long blockId = checklistBlockRepository.findBlockId(command.chkBlockId());
        domainEventPublisher.publish(ActivityOccurredEvent.of(
                ActivityLogAction.CREATE,
                blockId,
                created.getChkId(),
                created.getContent(),
                command.userId(),
                List.of(new ActivityFieldChange(null, null, null))
        ));

        return new CreateChecklistItemView(
                command.chkBlockId(),
                created.getChkId(),
                created.getContent(),
                completedCount,
                totalCount,
                created.getCreatedAt()
        );
    }

    @Override
    public UpdateChecklistItemView update(UpdateChecklistItemCommand command) {
        log.info("체크리스트 항목 수정 요청 - chkId={}, userId={}", command.chkId(), command.userId());

        if (command.content() == null && command.changeStatusTo() == null) {
            throw new ValidationException(ChecklistErrorCode.NO_FIELD_TO_UPDATE);
        }
        if (command.content() != null && command.content().isBlank()) {
            throw new ValidationException(ChecklistErrorCode.INVALID_CONTENT);
        }

        ChecklistItem before = eligibilityPolicy.getActiveItemOrThrow(command.chkId());
        eligibilityPolicy.assertEditPermission(before.getChkBlockId(), command.userId(), command.role());

        ChecklistItem updated = checklistRepository.updateFields(
                command.chkId(), command.content(), command.changeStatusTo());
        ChecklistRepository.CountSummary summary = checklistRepository.countSummary(updated.getChkBlockId());
        int completedCount = summary.completedCount();
        int totalCount = summary.totalCount();

        log.info("체크리스트 항목 수정 완료 - chkId={}", updated.getChkId());

        // 활동 로그(항목 수정, §5.2) — 실제로 바뀐 필드 단위로 남긴다. 둘 다 바뀌면 changes 에 2개가 담긴다.
        List<ActivityFieldChange> changes = new ArrayList<>();
        if (!Objects.equals(before.getContent(), updated.getContent())) {
            changes.add(new ActivityFieldChange("content", before.getContent(), updated.getContent()));
        }
        if (before.isCompleted() != updated.isCompleted()) {
            changes.add(new ActivityFieldChange("isCompleted",
                    String.valueOf(before.isCompleted()), String.valueOf(updated.isCompleted())));
        }
        if (!changes.isEmpty()) {
            Long blockId = checklistBlockRepository.findBlockId(updated.getChkBlockId());
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.MODIFY, blockId, updated.getChkId(), updated.getContent(),
                    command.userId(), changes
            ));
        }

        return new UpdateChecklistItemView(
                updated.getChkId(),
                updated.getContent(),
                updated.isCompleted(),
                completedCount,
                totalCount,
                updated.getUpdatedAt()
        );
    }

    @Override
    public DeleteChecklistItemView delete(DeleteChecklistItemCommand command) {
        log.info("체크리스트 항목 삭제 요청 - chkId={}, userId={}", command.chkId(), command.userId());

        ChecklistItem before = eligibilityPolicy.getActiveItemOrThrow(command.chkId());
        eligibilityPolicy.assertEditPermission(before.getChkBlockId(), command.userId(), command.role());

        boolean deleted = checklistRepository.markDeleted(command.chkId(), LocalDateTime.now());
        if (!deleted) {
            // 조회~삭제 사이에 동시 삭제된 경우. 존재하지 않는 항목과 동일하게 취급한다.
            log.warn("체크리스트 항목 삭제 경합 발생 - 이미 삭제됨 - chkId={}", command.chkId());
            throw new NotFoundException(ChecklistErrorCode.ITEM_NOT_FOUND);
        }

        ChecklistRepository.CountSummary summary = checklistRepository.countSummary(before.getChkBlockId());
        int completedCount = summary.completedCount();
        int totalCount = summary.totalCount();

        log.info("체크리스트 항목 삭제 완료 - chkId={}", command.chkId());

        // 활동 로그(항목 삭제, §5.2) — resourceName 에 삭제 전 항목 내용을 스냅샷으로 남긴다.
        Long blockId = checklistBlockRepository.findBlockId(before.getChkBlockId());
        domainEventPublisher.publish(ActivityOccurredEvent.of(
                ActivityLogAction.DELETE,
                blockId,
                before.getChkId(),
                before.getContent(),
                command.userId(),
                List.of(new ActivityFieldChange(null, null, null))
        ));

        return new DeleteChecklistItemView(completedCount, totalCount);
    }
}
