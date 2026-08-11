package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code ApprovalBlockDetailAdapter}(결재의 {@code BlockDetailPort} 구현)가 위임하는 실제 로직.
 * Checklist·Text의 {@code ChecklistHandlerService}/{@code TextHandlerService}와 동일한 역할 —
 * 블록 생성·삭제 트랜잭션 안에서 호출된다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApprovalHandlerService {

    private static final String CANCELED_NOTIFICATION_TYPE = "APPROVAL_CANCELED";
    private static final String CANCELED_NOTIFICATION_TITLE = "결재 취소";

    private final ApprovalRepository approvalRepository;
    private final BlockCatalogPort blockCatalogPort;
    private final EmployeeCatalogPort employeeCatalogPort;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 블록 생성 시 1회차 DRAFT {@code approval}을 만든다(APR-001). 기안자는 블록을 만든 사람으로
     * 정한다 — {@code createDetail(Long blockId)}엔 요청자가 따로 안 넘어온다.
     */
    public Long create(Long blockId) {
        BlockSummary block = blockCatalogPort.findBlock(blockId)
                .orElseThrow(() -> new IllegalStateException(
                        "block not found right after creation - blockId=" + blockId));

        EmployeeSummary creator = employeeCatalogPort.findEmployee(block.createdBy())
                .orElseThrow(() -> new ForbiddenException(ApprovalErrorCode.APPROVAL_NOT_DRAFTER));
        if (creator.participationUnavailable() || "ADMIN".equals(creator.role())) {
            throw new ForbiddenException(ApprovalErrorCode.APPROVAL_NOT_DRAFTER);
        }

        Long approvalId = approvalRepository.createDraft(blockId, block.createdBy()).approval().getApprovalId();
        log.info("결재 상세 생성 - blockId={}, approvalId={}, drafterId={}", blockId, approvalId, block.createdBy());
        return approvalId;
    }

    /**
     * 블록 삭제 시 호출된다(APR-001-2 · INV-09). 결재는 블록에서만 올라가므로 블록이 사라지면
     * 그 결재도 함께 사라진다 — 문서 · 결재선 · 회차 · 결재 순으로 논리 삭제한다.
     *
     * <p>⛔ <b>진행 중이어도 막지 않는다</b> (2026-08-10 · BLK-008 삭제 잠금 폐기 반영).
     * 막으면 상위 스텝 삭제까지 통째로 409 로 롤백돼, 결재 하나 때문에 스텝을 못 지우면서
     * 사용자에게는 탈출구가 없었다. 살리고 싶은 블록은 다른 스텝으로 옮긴다 (BLK-014).
     *
     * <p>DEL-006/013: 상태 전이와 같은 부모 {@code approval} 행을 먼저 잠근다. 대상이 없거나 이미
     * 삭제됐으면 성공으로 끝내 재시도와 불완전한 과거 데이터가 상위 블록 삭제를 막지 않게 한다.
     * 멱등 종료 경로에서는 알림도 다시 발행하지 않는다.
     */
    public void deleteByBlock(Long approvalId, String userId, String blockTitle, LocalDateTime deletedAt) {
        log.info("결재 상세 삭제(블록 삭제 트랜잭션) - approvalId={}, userId={}", approvalId, userId);

        Optional<Approval> found = approvalRepository.findApprovalIncludingDeletedForUpdate(approvalId);
        if (found.isEmpty() || found.get().getDeletedAt() != null) {
            log.info("결재 상세 삭제 멱등 종료 - approvalId={}", approvalId);
            return;
        }
        Approval approval = found.get();

        // ⚠️ 수신자·제목을 삭제 전에 읽는다. 지운 뒤에 읽으면 활성 조회 필터(DEL-007)에 걸려
        //    수신자가 0명이 되는데, 예외도 로그도 없이 알림만 조용히 안 나간다.
        List<String> recipients = resolveCancelRecipients(approval);
        String approvalTitle = resolveApprovalTitle(approvalId, blockTitle);

        approvalRepository.softDeleteCascade(approvalId, deletedAt);

        publishCanceledNotifications(recipients, approvalTitle, approvalId);

        log.info("결재 상세 삭제 완료 - approvalId={}, 취소 알림 수신자={}", approvalId, recipients.size());
    }

    /**
     * DEL-011 — 진행 중 결재의 기안자와 <b>지금 차례인 결재자</b>에게만 알린다.
     *
     * <p>{@code DRAFT}는 아직 아무에게도 결재 요청이 가지 않았고, {@code COMPLETED}/{@code REJECTED}는
     * 이미 결과를 통지받았으므로 대상이 아니다. 기다리던 결재가 사라진 사람만 알아야 한다.
     * 처리를 마친 결재자({@code APPROVED})도 제외한다 — 이미 자기 몫이 끝났다.
     */
    private List<String> resolveCancelRecipients(Approval approval) {
        if (approval.getStatus() != ApprovalStatus.IN_PROGRESS) {
            return List.of();
        }

        Set<String> recipients = new LinkedHashSet<>();
        recipients.add(approval.getActingDrafterId() != null
                ? approval.getActingDrafterId() : approval.getDrafterId());
        approvalRepository.findLinesByApprovalId(approval.getApprovalId()).stream()
                .filter(line -> line.getStatus() == ApprovalLineStatus.ACTIVE)
                .map(ApprovalLine::getApproverId)
                .forEach(recipients::add);

        return new ArrayList<>(recipients);
    }

    /** 알림 문구용 제목. 결재 제목이 우선이고, 없으면 블록 제목으로 대체한다. */
    private String resolveApprovalTitle(Long approvalId, String blockTitle) {
        String revisionTitle = approvalRepository.findLatestRevisionReadOnly(approvalId)
                .map(revision -> revision.getTitle())
                .filter(title -> title != null && !title.isBlank())
                .orElse(null);
        if (revisionTitle != null) {
            return revisionTitle;
        }
        return (blockTitle == null || blockTitle.isBlank()) ? "결재" : blockTitle;
    }

    /**
     * ⛔ 이동 대상을 붙이지 않는다. 결재가 이미 삭제돼 상세 조회가 404 이므로(DEL-008), 대상을 주면
     * 알림을 눌렀을 때 에러 화면으로 보낸다. 조회는 {@code type=NONE} 을 돌려주고 프론트는 이동시키지 않는다.
     */
    private void publishCanceledNotifications(List<String> recipients, String approvalTitle, Long approvalId) {
        String message = approvalTitle + " 결재가 취소되었습니다. 상위 항목이 삭제되어 더 진행할 수 없습니다.";
        recipients.forEach(recipient -> {
            domainEventPublisher.publish(NotificationRequestedEvent.of(
                    recipient, CANCELED_NOTIFICATION_TYPE, CANCELED_NOTIFICATION_TITLE, message));
            log.info("결재 취소 알림 발행 - approvalId={}, recipient={}", approvalId, recipient);
        });
    }
}
