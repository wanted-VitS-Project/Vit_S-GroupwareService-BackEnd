package com.group3.vitamins.approval.infrastructure.notification;

import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.notification.application.port.NotificationTarget;
import com.group3.vitamins.notification.application.port.NotificationTargetResolverPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * APPROVAL 타입 블록의 이동 대상 조회 어댑터(VIW-006) — {@code type=APPROVAL} 일 때
 * {@code targetId=approvalId}, {@code extra.revisionId} 를 채운다.
 *
 * <p>알림의 {@code NotificationTargetResolverPort} SPI 구현체지만, 결재 모듈 안에 둔다 —
 * {@code project.block.application.port.BlockDetailPort} 구현체들(`approval.infrastructure.blockdetail`,
 * `checklist.infrastructure.blockdetail`, `text.infrastructure.blockdetail`)이 전부 포트 소유자(block)가
 * 아니라 **타입 담당 도메인 자신의 패키지**에 있는 것과 동일한 이유(`ARCHITECTURE.md` §2-2) — 새 도메인이
 * 이동 대상 조회를 지원하려면 자기 모듈에 어댑터 하나만 추가하면 되고, {@code notification} 패키지를
 * 건드릴 필요가 없다.
 *
 * <p>{@code typeId}(= {@code block.type_id})는 결재 블록 생성 시(`ApprovalHandlerService.create`)
 * {@code approvalId} 그 자체로 연결되므로 그대로 쓴다. 읽기 전용 최신 회차 조회는
 * {@code ApprovalBlockDetailAdapter.loadDetails} 가 쓰는 것과 같은 잠금 없는 메서드를 재사용한다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalNotificationTargetAdapter implements NotificationTargetResolverPort {

    private static final String TYPE = "APPROVAL";

    private final ApprovalRepository approvalRepository;

    @Override
    public String supportedType() {
        return TYPE;
    }

    @Override
    public Optional<NotificationTarget> resolve(Long typeId) {
        Long approvalId = typeId;
        return approvalRepository.findLatestRevisionReadOnly(approvalId)
                .map(ApprovalRevision::getRevisionId)
                .map(revisionId -> NotificationTarget.of(approvalId, Map.of("revisionId", revisionId)));
    }
}
