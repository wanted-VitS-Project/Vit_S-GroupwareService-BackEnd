package com.group3.vitamins.approval.infrastructure.blockdetail;

import com.group3.vitamins.approval.application.service.ApprovalHandlerService;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.result.ApprovalDetail;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.domain.model.BlockType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * APPROVAL 블록의 {@link BlockDetailPort} 구현. 결재 상세 생성·삭제(APR-001·APR-001-2)는
 * 별도 REST API가 아니라 블록 생성·삭제와 같은 트랜잭션에서 여기로 들어온다.
 *
 * <p>생성·삭제 로직은 {@link ApprovalHandlerService}에, 미리보기 조회는 {@link ApprovalDetailMapper}
 * (MyBatis)에 위임하는 얇은 어댑터다 — Checklist·Text의
 * {@code ChecklistBlockDetailAdapter}/{@code TextBlockDetailAdapter}와 동일한 패턴.
 */
@Component
@RequiredArgsConstructor
public class ApprovalBlockDetailAdapter implements BlockDetailPort {

    private final ApprovalHandlerService approvalHandlerService;
    private final ApprovalDetailMapper approvalDetailMapper;

    @Override
    public BlockType supportedType() {
        return BlockType.APPROVAL;
    }

    @Override
    public Long createDetail(Long blockId) {
        return approvalHandlerService.create(blockId);
    }

    @Override
    public void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt) {
        approvalHandlerService.deleteByBlock(typeId, userId, blockTitle, deletedAt);
    }

    /** 블록 카드 미리보기(BND-003) — 최신 회차 기준 결재선 진행 현황만 가볍게 담는다 */
    @Override
    public Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }

        List<ApprovalRevisionRow> revisions = approvalDetailMapper.findLatestRevisions(typeIds);
        List<Long> revisionIds = revisions.stream().map(ApprovalRevisionRow::revisionId).toList();
        Map<Long, List<ApprovalLineRow>> linesByRevision = revisionIds.isEmpty()
                ? Map.of()
                : approvalDetailMapper.findLinesByRevisionIds(revisionIds).stream()
                        .collect(Collectors.groupingBy(ApprovalLineRow::revisionId));

        Map<Long, BlockDetail> details = new HashMap<>();
        for (ApprovalRevisionRow revision : revisions) {
            List<ApprovalLineRow> lines = linesByRevision.getOrDefault(revision.revisionId(), List.of());
            int approvedCount = (int) lines.stream()
                    .filter(line -> "APPROVED".equals(line.status()))
                    .count();
            details.put(revision.approvalId(), new ApprovalDetail(
                    revision.approvalId(), revision.revisionId(), revision.revisionNo(),
                    revision.status(), lines.size(), approvedCount));
        }
        return details;
    }
}
