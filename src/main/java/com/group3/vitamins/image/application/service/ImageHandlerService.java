package com.group3.vitamins.image.application.service;

import com.group3.vitamins.image.domain.repository.ImageBlockRepository;
import com.group3.vitamins.image.domain.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ImageHandlerService {

    private final ImageBlockRepository imageBlockRepository;
    private final ImageRepository imageRepository;

    /**
     * 이미지 블록 삭제 이벤트 수신.
     *
     * <p>텍스트·체크리스트 도메인이 자기 소유 detail 행의 deleted_at 을 직접 찍는 것과 동일하게,
     * 이 도메인도 자기 소유 detail 행(image_block)의 deleted_at 을 직접 찍는다. 다만 이미지는
     * 체크리스트와 동일하게 1:N 이라 그 블록에 속한 항목(image)들까지 함께 정리해야 한다.
     */
    public void deleteByBlock(Long imgBlockId, String userId, String blockTitle, LocalDateTime deletedAt) {
        log.info("이미지 블록 삭제(이벤트 수신) - imgBlockId={}, userId={}", imgBlockId, userId);

        // deletedAt 이 null 이면 "WHERE deleted_at IS NULL" 조건에 걸려 SET deleted_at = NULL 로
        // 아무 값도 안 바뀌었는데 영향받은 행이 있다는 이유로 삭제 성공(true)으로 오판할 수 있다.
        Objects.requireNonNull(deletedAt, "deletedAt은 null일 수 없습니다.");

        boolean blockDeleted = imageBlockRepository.markDeleted(imgBlockId, deletedAt);
        if (!blockDeleted) {
            // 조건부 UPDATE가 0건 갱신 = 이미 삭제돼 있었다는 뜻. 중복 이벤트로 판단하고 멱등하게 무시한다.
            log.info("이미 삭제 처리된 이미지 블록 - 중복 이벤트로 판단하고 무시 - imgBlockId={}", imgBlockId);
            return;
        }

        int deletedItemCount = imageRepository.markAllDeletedByBlock(imgBlockId, deletedAt);

        log.info("이미지 블록 삭제 완료 - imgBlockId={}, 삭제된 항목 수={}", imgBlockId, deletedItemCount);

        // TODO: 활동 로그(블록 삭제) 이벤트 발행 — resourceId=null, 기록 정보=삭제 전 Block명 (§5.1 Block 공통).
        //       블록에 속한 항목 각각의 삭제는 별도로 로그를 남기지 않는다 — Block 삭제 활동으로 대표된다.
        //       블록 삭제 이벤트 타입이 아직 확정 안 돼서(동훈님) 리스너 자체가 주석 처리 상태라
        //       이 메서드가 실제로 호출되는 시점은 아직 없다 — 이벤트 타입 확정되면 같이 정리할 것.
        // domainEventPublisher.publish(ActivityOccurredEvent.of(
        //         ActivityLogAction.DELETE, blockId, null, userId,
        //         List.of(new ActivityFieldChange(null, null, null))
        // ));
    }
}
