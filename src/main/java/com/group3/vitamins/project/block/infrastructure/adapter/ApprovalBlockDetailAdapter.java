package com.group3.vitamins.project.block.infrastructure.adapter;

import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.model.ApprovalWithRevision;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.result.ApprovalDetail;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * APPROVAL 블록의 {@link BlockDetailPort} 구현. 결재 상세 생성(APR-001)이 별도 REST API가 아니라
 * 여기로 옮겨왔다 — 블록 생성(`BlockCommandService.createBlock`)과 같은 트랜잭션에서 호출된다.
 *
 * <p>구 {@code POST /api/v1/blocks/{blockId}/approval}(#88)은 이 어댑터로 대체되어 삭제됐다.
 * Text·Checklist도 자체 생성 API가 없고 이 방식(`TextBlockDetailAdapter`/`ChecklistBlockDetailAdapter`)을
 * 쓰는 것과 동일한 패턴이다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalBlockDetailAdapter implements BlockDetailPort {

    private final ApprovalRepository approvalRepository;
    private final BlockRepository blockRepository;

    @Override
    public BlockType supportedType() {
        return BlockType.APPROVAL;
    }

    /**
     * {@code approval}(DRAFT) + 1회차 {@code approval_revision}을 생성하고 {@code approval_id}를
     * 돌려준다({@code block.type_id}가 됨). 기안자는 {@code createDetail}에 요청자가 안 넘어와서
     * {@code block.created_by}로 정한다 — 블록 생성 트랜잭션 안에서 이미 스텝 편집 권한이
     * 확인된 사람이라 별도 프로젝트 member 검증도 다시 하지 않는다.
     */
    @Override
    public Long createDetail(Long blockId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new IllegalStateException(
                        "block not found right after creation - blockId=" + blockId));

        ApprovalWithRevision created = approvalRepository.createDraft(blockId, block.getCreatedBy());
        return created.approval().getApprovalId();
    }

    /**
     * 블록 삭제와 같은 트랜잭션에서 호출된다(APR-001-2 · INV-09). 진행 중인 결재는 여기서 예외를
     * 던져 블록 삭제 자체를 막는다 — 별도 삭제 확인 API를 두지 않는다.
     */
    @Override
    public void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt) {
        Approval approval = approvalRepository.findApproval(typeId)
                .orElseThrow(() -> new IllegalStateException("approval not found for typeId=" + typeId));

        if (approval.getStatus() == ApprovalStatus.IN_PROGRESS) {
            throw new ConflictException(ApprovalErrorCode.APPROVAL_IN_PROGRESS);
        }

        approvalRepository.softDeleteCascade(typeId, deletedAt);
    }

    /** 블록 카드 미리보기(BND-003) — 최신 회차 기준 결재선 진행 현황만 가볍게 담는다 */
    @Override
    public Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, BlockDetail> details = new HashMap<>();
        for (Long approvalId : typeIds) {
            approvalRepository.findApproval(approvalId).ifPresent(approval -> {
                // 읽기 전용 호출이라 findLatestRevisionReadOnly(락 없음)를 쓴다 — findLatestRevision은
                // @Lock(PESSIMISTIC_WRITE)라 읽기 전용 트랜잭션에서 부르면 DB가 거부한다
                Optional<ApprovalRevision> latestRevision = approvalRepository.findLatestRevisionReadOnly(approvalId);
                List<ApprovalLine> lines = latestRevision
                        .map(revision -> approvalRepository.findLinesByRevisionId(revision.getRevisionId()))
                        .orElse(List.of());
                int approvedCount = (int) lines.stream()
                        .filter(line -> line.getStatus() == ApprovalLineStatus.APPROVED)
                        .count();
                details.put(approvalId, new ApprovalDetail(
                        approvalId,
                        latestRevision.map(ApprovalRevision::getRevisionId).orElse(null),
                        latestRevision.map(ApprovalRevision::getRevisionNo).orElse(0),
                        approval.getStatus().name(), lines.size(), approvedCount));
            });
        }
        return details;
    }
}
