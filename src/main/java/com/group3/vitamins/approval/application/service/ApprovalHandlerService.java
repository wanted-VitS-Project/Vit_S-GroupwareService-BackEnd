package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    private final ApprovalRepository approvalRepository;
    private final BlockCatalogPort blockCatalogPort;

    /**
     * 블록 생성 시 1회차 DRAFT {@code approval}을 만든다(APR-001). 기안자는 블록을 만든 사람으로
     * 정한다 — {@code createDetail(Long blockId)}엔 요청자가 따로 안 넘어온다.
     */
    public Long create(Long blockId) {
        BlockSummary block = blockCatalogPort.findBlock(blockId)
                .orElseThrow(() -> new IllegalStateException(
                        "block not found right after creation - blockId=" + blockId));

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
     */
    public void deleteByBlock(Long approvalId, String userId, String blockTitle, LocalDateTime deletedAt) {
        log.info("결재 상세 삭제(블록 삭제 트랜잭션) - approvalId={}, userId={}", approvalId, userId);

        var approval = approvalRepository.findApprovalIncludingDeletedForUpdate(approvalId);
        if (approval.isEmpty() || approval.get().getDeletedAt() != null) {
            log.info("결재 상세 삭제 멱등 종료 - approvalId={}", approvalId);
            return;
        }

        approvalRepository.softDeleteCascade(approvalId, deletedAt);
        log.info("결재 상세 삭제 완료 - approvalId={}", approvalId);
    }
}
