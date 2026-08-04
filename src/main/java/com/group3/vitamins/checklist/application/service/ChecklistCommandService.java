package com.group3.vitamins.checklist.application.service;

import com.group3.vitamins.checklist.application.command.CreateChecklistItemCommand;
import com.group3.vitamins.checklist.application.command.DeleteChecklistItemCommand;
import com.group3.vitamins.checklist.application.command.UpdateChecklistItemCommand;
import com.group3.vitamins.checklist.application.policy.ChecklistEligibilityPolicy;
import com.group3.vitamins.checklist.application.usecase.ChecklistCommandUseCase;
import com.group3.vitamins.checklist.domain.exception.ChecklistErrorCode;
import com.group3.vitamins.checklist.domain.model.ChecklistItem;
import com.group3.vitamins.checklist.domain.repository.ChecklistRepository;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 체크리스트 블록 생성·삭제는 Block 도메인(동훈님)이 처리한다 — 여기는 항목(내부 데이터) 생성·수정·삭제만 담당한다.
 * 블록 삭제 시 소속 항목 정리는 {@link ChecklistHandlerService} 가 이벤트 리스너를 통해 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChecklistCommandService implements ChecklistCommandUseCase {

    private final ChecklistEligibilityPolicy eligibilityPolicy;
    private final ChecklistRepository checklistRepository;

    @Override
    public CreateChecklistItemView create(CreateChecklistItemCommand command) {
        log.info("체크리스트 항목 생성 요청 - chkBlockId={}, userId={}", command.chkBlockId(), command.userId());

        eligibilityPolicy.assertBlockActiveOrThrow(command.chkBlockId());
        eligibilityPolicy.assertEditPermission(command.chkBlockId(), command.userId());

        ChecklistItem created = checklistRepository.create(command.chkBlockId(), command.content());
        int completedCount = checklistRepository.countCompletedActiveItems(command.chkBlockId());
        int totalCount = checklistRepository.countActiveItems(command.chkBlockId());

        log.info("체크리스트 항목 생성 완료 - chkId={}", created.getChkId());

        // TODO: 활동 로그(항목 생성) 이벤트 발행 — 활동 로그 인프라(ActivityOccurredEvent 등)가 아직
        //       실제로 만들어지지 않아 주석으로만 남긴다 (§5.2 체크리스트). resourceId=chkId, 기록 정보=생성된 항목 내용.
        //       ActivityOccurredEvent 는 공용 block 테이블의 blockId 를 요구하는데, 이 도메인은 chkBlockId(블록
        //       상세 PK)만 들고 있다 — 실제 연동 시 BlockCatalogPort 등으로 chkBlockId → blockId 매핑이 필요하다.
        // activityEventPublisher.publish(
        //         ActivityOccurredEvent.created(blockId, created.getChkId(), command.userId(), created.getContent())
        // );

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

        ChecklistItem before = eligibilityPolicy.getActiveItemOrThrow(command.chkId());
        eligibilityPolicy.assertEditPermission(before.getChkBlockId(), command.userId());

        ChecklistItem updated = checklistRepository.updateFields(
                command.chkId(), command.content(), command.changeStatusTo());
        int completedCount = checklistRepository.countCompletedActiveItems(updated.getChkBlockId());
        int totalCount = checklistRepository.countActiveItems(updated.getChkBlockId());

        log.info("체크리스트 항목 수정 완료 - chkId={}", updated.getChkId());

        // TODO: 활동 로그(항목 수정) 이벤트 발행 — 변경된 필드 단위로 로그를 남긴다 (§5.2 체크리스트).
        //       활동 로그 인프라가 아직 없어 주석으로만 남긴다. 요청값과 기존 값이 같으면 로그를 남기지 않는다.
        // List<ActivityFieldChange> changes = new ArrayList<>();
        // if (!Objects.equals(before.getContent(), updated.getContent())) {
        //     changes.add(new ActivityFieldChange("content", before.getContent(), updated.getContent()));
        // }
        // if (before.isCompleted() != updated.isCompleted()) {
        //     changes.add(new ActivityFieldChange("isCompleted",
        //             String.valueOf(before.isCompleted()), String.valueOf(updated.isCompleted())));
        // }
        // if (!changes.isEmpty()) {
        //     activityEventPublisher.publish(
        //             ActivityOccurredEvent.modified(blockId, updated.getChkId(), command.userId(), changes)
        //     );
        // }

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
        eligibilityPolicy.assertEditPermission(before.getChkBlockId(), command.userId());

        boolean deleted = checklistRepository.markDeleted(command.chkId(), LocalDateTime.now());
        if (!deleted) {
            // 조회~삭제 사이에 동시 삭제된 경우. 존재하지 않는 항목과 동일하게 취급한다.
            log.warn("체크리스트 항목 삭제 경합 발생 - 이미 삭제됨 - chkId={}", command.chkId());
            throw new NotFoundException(ChecklistErrorCode.ITEM_NOT_FOUND);
        }

        int completedCount = checklistRepository.countCompletedActiveItems(before.getChkBlockId());
        int totalCount = checklistRepository.countActiveItems(before.getChkBlockId());

        log.info("체크리스트 항목 삭제 완료 - chkId={}", command.chkId());

        // TODO: 활동 로그(항목 삭제) 이벤트 발행 — 활동 로그 인프라(ActivityOccurredEvent 등)가 아직
        //       실제로 만들어지지 않아 주석으로만 남긴다. resourceId=chkId, beforeValue=삭제 전 항목 내용 (§5.2 체크리스트).
        // activityEventPublisher.publish(
        //         ActivityOccurredEvent.deleted(blockId, before.getChkId(), command.userId(), before.getContent())
        // );

        return new DeleteChecklistItemView(completedCount, totalCount);
    }
}
