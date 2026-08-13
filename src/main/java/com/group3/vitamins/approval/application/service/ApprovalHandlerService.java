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

    /** 제목을 못 찾았을 때 문구에 넣을 대체어 */
    private static final String TITLE_FALLBACK = "결재";

    /**
     * DEL-016 — 블록 직접 삭제 전에 <b>확인을 요구하는</b> 상태와 그때 보일 문구. 상신 이후는 전부 여기 든다.
     *
     * <p>확인 요구 상태 목록과 문구를 <b>한 맵에 둔다</b> — 따로 두면 상태를 추가할 때 문구를 빠뜨려
     * 엉뚱한 기본 문구가 나간다. 여기 넣으면 문구가 강제된다.
     *
     * <p>⚠️ <b>문구는 상태마다 다르고, 각자 그 상태에서 실제로 잃는 것을 말한다.</b> 사용자는 지금 화면의
     * 상태를 기준으로 읽는다 — 완료 결재에 "취소됩니다"라고 하면 틀린 말이다(취소가 아니라 이력 열람을
     * 잃는다). <b>코드는 하나로 유지한다</b> — 처리가 셋 다 같아 쪼개면 프론트가 같은 분기를 세 번 짠다.
     *
     * <p>{@code %s}는 결재 제목이다. ⛔ <b>결재자 이름은 넣지 않는다</b> — ① 결재선의 사람 정보는
     * {@code ApprovalViewPolicy}가 기안자·{@code ACTIVE} 이상 결재자·{@code MASTER}로 제한하는데
     * 삭제 권한자는 스텝 EDITOR라, 조회로 막아둔 정보를 에러 메시지로 흘리게 된다(블록 카드도 집계만
     * 내려주고 이름은 안 준다) ② 퇴사·계정 비활성 사원이 {@code ACTIVE}로 남아 있을 수 있어 없는 사람을
     * 지목한다 ③ 확인 다이얼로그를 띄운 뒤 확정까지 사이에 승인이 나면 이미 틀린 값이 된다.
     * 제목은 블록 카드에 이미 나가는 값이라 새로 노출되는 것이 없다.
     */
    private static final Map<ApprovalStatus, String> CONFIRM_MESSAGES = new EnumMap<>(Map.of(
            ApprovalStatus.IN_PROGRESS, "%s 결재가 진행 중입니다. 삭제하면 결재가 취소됩니다.",
            ApprovalStatus.REJECTED, "%s 결재는 반려된 상태입니다. 삭제하면 재상신할 수 없습니다.",
            ApprovalStatus.COMPLETED, "%s 결재는 완료된 상태입니다. 삭제하면 승인 이력을 다시 볼 수 없습니다."));

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
     * DEL-016 — <b>블록 직접 삭제</b> 전에 확인이 필요한지 판정한다. 상신 이후 결재는 <b>막지 않고
     * 되묻는다</b> — {@code confirmed}가 {@code false}면 409로 무엇을 잃는지 알리고, 사용자가 확인해
     * 다시 요청하면({@code confirmed=true}) 그대로 삭제한다.
     *
     * <p>{@code DRAFT}는 아직 아무에게도 요청이 가지 않아, {@code CANCELED}는 이미 종결돼 확인 없이
     * 통과한다. 대상이 없거나 이미 삭제됐으면 {@link #deleteByBlock}과 같은 멱등 판정으로 통과한다.
     *
     * <p>회차({@code approval_revision.status})가 아니라 <b>부모 결재 상태</b>로 판정한다 — 재상신 중이면
     * 회차는 {@code DRAFT}인데 결재는 {@code REJECTED}라, 회차로 보면 반려 결재가 확인 없이 지워진다.
     *
     * <p>⛔ 스텝 삭제 cascade 는 이 메서드를 부르지 않는다 (DEL-017).
     */
    public void assertDeletableByBlock(Long approvalId, String blockTitle, boolean confirmed) {
        if (confirmed) {
            return;
        }

        Optional<Approval> found = approvalRepository.findApprovalIncludingDeletedForUpdate(approvalId);
        if (found.isEmpty() || found.get().getDeletedAt() != null) {
            return;
        }

        ApprovalStatus status = found.get().getStatus();
        String template = CONFIRM_MESSAGES.get(status);
        if (template == null) {
            return;
        }

        log.info("상신된 결재의 블록 직접 삭제 - 확인 요구 approvalId={}, status={}", approvalId, status);
        throw new ConflictException(ApprovalErrorCode.APPROVAL_DELETE_CONFIRM_REQUIRED,
                template.formatted(resolveApprovalTitle(approvalId, blockTitle)));
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
        return (blockTitle == null || blockTitle.isBlank()) ? TITLE_FALLBACK : blockTitle;
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
