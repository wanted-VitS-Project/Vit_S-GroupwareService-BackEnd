package com.group3.vitamins.checklist.infrastructure.event;

import com.group3.vitamins.checklist.application.service.ChecklistHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChecklistLifeCycleEventHandler {
    private final ChecklistHandlerService checklistHandlerService;

//    //체크리스트 블록 삭제 이벤트 리스너
//    @Async("domainEventExecutor")
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handleDeleteChecklistBlock( event) {
//        log.info("[checklistLifecycleEventHandler] 체크리스트 블록 삭제로 인한 세부 항목 삭제 처리");
//
//        checklistHandlerService.deleteByBlock(
//                event.chkBlockId(), //체크리스트 블록 아이디
//                event.userId(), //삭제 주체자
//                event.blockTitle(), //삭제된 블록 제목
//                event.deletedAt() //삭제일
//        );
//    }
}
