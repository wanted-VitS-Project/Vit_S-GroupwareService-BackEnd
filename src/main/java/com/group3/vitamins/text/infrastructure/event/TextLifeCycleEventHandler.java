package com.group3.vitamins.text.infrastructure.event;

import com.group3.vitamins.text.application.service.TextHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TextLifeCycleEventHandler {
    private final TextHandlerService textHandlerService;

//    //텍스트 블록 삭제 이벤트 리스너
//    @Async("domainEventExecutor")
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handleDeleteTextBlock( event) {
//        log.info("[textLifecycleEventHandler] 텍스트 블록 삭제로 인한 세부 내용 삭제 처리");
//
//        textHandlerService.delete(
//                event.txtId(), //하나의 블록 아이디
//                event.userId(), //삭제 주체자
//                event.blockTitle(), //삭제된 블록 아이디
//                event.deletedAt() //삭제일
//        );
//    }
}
