package com.group3.vitamins.image.infrastructure.event;

import com.group3.vitamins.image.application.service.ImageHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageLifeCycleEventHandler {
    private final ImageHandlerService imageHandlerService;

//    //이미지 블록 삭제 이벤트 리스너
//    @Async("domainEventExecutor")
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void handleDeleteImageBlock( event) {
//        log.info("[imageLifecycleEventHandler] 이미지 블록 삭제로 인한 세부 항목 삭제 처리");
//
//        imageHandlerService.deleteByBlock(
//                event.imgBlockId(), //이미지 블록 아이디
//                event.userId(), //삭제 주체자
//                event.blockTitle(), //삭제된 블록 제목
//                event.deletedAt() //삭제일
//        );
//    }
}
