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
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    /**
     * DEL-016 — 블록 직접 삭제를 막는 상태와 그때 사용자에게 보일 문구. 상신 이후는 전부 여기 든다.
     *
     * <p>막는 상태 목록과 문구를 <b>한 맵에 둔다</b> — 따로 두면 상태를 추가할 때 문구를 빠뜨려
     * "이미 상신된 결재는…"이라는 엉뚱한 기본 문구가 나간다. 여기 넣으면 문구가 강제된다.
     *
     * <p>⚠️ 문구가 상태마다 다른 이유: 사용자는 <b>지금 화면의 상태</b>를 기준으로 읽는다. 반려 블록에
     * "상신된 결재"라고 하면 다른 것을 가리키는 것처럼 읽힌다. <b>코드는 하나로 유지한다</b> —
     * 처리 방식이 셋 다 같아서 쪼개면 프론트가 같은 분기를 세 번 짠다 ({@code DomainException} 참고).
     */
    private static final Map<ApprovalStatus, String> LOCKED_MESSAGES = new EnumMap<>(Map.of(
            ApprovalStatus.IN_PROGRESS, "진행 중인 결재는 삭제할 수 없습니다.",
            ApprovalStatus.REJECTED, "반려된 결재는 삭제할 수 없습니다.",
            ApprovalStatus.COMPLETED, "완료된 결재는 삭제할 수 없습니다."));

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
     * DEL-016 — <b>블록 직접 삭제</b>가 허용되는지 판정한다. 한 번 상신된 결재는 막는다.
     *
     * <p>{@code DRAFT}는 아직 아무에게도 요청이 가지 않아 통과하고, {@code CANCELED}는 이미 종결된
     * 건이라 통과한다. 대상이 없거나 이미 삭제됐으면 {@link #deleteByBlock}과 같은 멱등 판정으로 통과한다.
     *
     * <p>회차({@code approval_revision.status})가 아니라 <b>부모 결재 상태</b>로 판정한다 — 재상신 중이면
     * 회차는 {@code DRAFT}인데 결재는 {@code REJECTED}라, 회차로 보면 반려 결재가 삭제 가능해진다.
     *
     * <p>⛔ 스텝 삭제 cascade 는 이 메서드를 부르지 않는다 (DEL-017).
     */
    public void assertDeletableByBlock(Long approvalId) {
        Optional<Approval> found = approvalRepository.findApprovalIncludingDeletedForUpdate(approvalId);
        if (found.isEmpty() || found.get().getDeletedAt() != null) {
            return;
        }

        ApprovalStatus status = found.get().getStatus();
        String message = LOCKED_MESSAGES.get(status);
        if (message == null) {
            return;
        }

        log.info("상신된 결재의 블록 직접 삭제 거부 - approvalId={}, status={}", approvalId, status);
        throw new ConflictException(ApprovalErrorCode.APPROVAL_ALREADY_SUBMITTED, message);
    }

    /**
     * 블록 삭제 시 호출된다(APR-001-2 · INV-09). 결재는 블록에서만 올라가므로 블록이 사라지면
     * 그 결재도 함께 사라진다 — 문서 · 결재선 · 회차 · 결재 순으로 논리 삭제한다.
     *
     * <p>⛔ <b>여기서는 상태를 보지 않는다.</b> 스텝 삭제 cascade 가 이 메서드로 들어오는데, 막으면
     * 상위 스텝 삭제까지 통째로 409 로 롤백돼 결재 하나 때문에 스텝을 못 지우게 된다 (BLK-008 폐기 사유).
     * 직접 삭제만 막는 판정은 {@link #assertDeletableByBlock}이 담당하고, 그건 블록 도메인이
     * <b>직접 삭제 경로에서만</b> 부른다 (DEL-016·DEL-017).
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
