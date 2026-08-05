package com.group3.vitamins.vitamate.infrastructure.blockdetail;

import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.VitamateDetail;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.vitamate.application.service.VitamateBlockHandlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

// 공통 Block 도메인에 AI 블록의 생성·삭제·상세 조회를 연결하는 어댑터
@Component
@RequiredArgsConstructor
public class VitamateBlockDetailAdapter implements BlockDetailPort {

    private final VitamateDetailMapper vitamateDetailMapper;
    private final VitamateBlockHandlerService vitamateBlockHandlerService;

    // 이 어댑터는 AI 블록 타입을 담당한다.
    @Override
    public BlockType supportedType() {
        return BlockType.AI;
    }

    // block 생성 트랜잭션 안에서 빈 vitamate_block 상세 행을 만든다.
    @Override
    public Long createDetail(Long blockId) {
        return vitamateBlockHandlerService.create(blockId);
    }

    // block 삭제 트랜잭션 안에서 vitamate_block 상세 행을 논리 삭제한다.
    @Override
    public void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt) {
        vitamateBlockHandlerService.delete(typeId, userId, blockTitle, deletedAt);
    }

    // AI 블록 상세를 typeId 기준으로 한 번에 조회한다.
    @Override
    public Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }

        return vitamateDetailMapper.findByVitamateBlockIds(typeIds).stream()
                .collect(Collectors.toMap(
                        VitamateDetailRow::vitamateBlockId,
                        row -> new VitamateDetail(row.vitamateBlockId(), row.welcomeMessage())
                ));
    }
}
