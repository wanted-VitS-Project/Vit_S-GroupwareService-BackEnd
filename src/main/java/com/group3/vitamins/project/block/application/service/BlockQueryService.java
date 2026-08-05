package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.port.BlockIssueStatLookupPort;
import com.group3.vitamins.project.block.application.query.BlockListQuery;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.BlockOwner;
import com.group3.vitamins.project.block.application.result.BlockSummary;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import com.group3.vitamins.project.block.application.usecase.BlockQueryUseCase;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockQueryService implements BlockQueryUseCase {

    private final BlockRepository blockRepository;
    private final BlockIssueStatLookupPort blockIssueStatLookupPort;
    private final EmployeeLookupPort employeeLookupPort;
    private final BlockDetailRegistry blockDetailRegistry;
    private final StepAccessUseCase stepAccessUseCase;

    @Override
    public List<BlockSummary> getBlocks(BlockListQuery query) {
        stepAccessUseCase.requireAccess(query.stepId(), query.requesterUserId(), query.role());

        List<Block> blocks = blockRepository.findByStepId(query.stepId());
        if (blocks.isEmpty()) {
            return List.of();
        }

        Map<Long, BlockIssueStatLookupPort.BlockIssueStat> issueStats = blockIssueStatLookupPort
                .countByBlockIds(blocks.stream().map(Block::getBlockId).toList());
        Map<String, String> names = employeeLookupPort.findNamesByUserIds(blocks.stream()
                .map(Block::getOwner)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<Long, BlockDetail> details = loadDetails(blocks);

        return blocks.stream()
                .map(block -> toSummary(block, issueStats, names, details))
                .toList();
    }

    /**
     * 타입별로 typeId 를 모아 어댑터마다 한 번씩 배치 조회하고, 결과를 blockId 키로 바꿔 담는다.
     * 담당 어댑터가 없는 타입은 아무것도 담지 않아 응답에서 detail 이 null 이 된다.
     */
    private Map<Long, BlockDetail> loadDetails(List<Block> blocks) {
        Map<BlockType, List<Block>> byType = blocks.stream()
                .filter(block -> block.getTypeId() != null)
                .collect(Collectors.groupingBy(Block::getType));

        Map<Long, BlockDetail> byBlockId = new HashMap<>();
        byType.forEach((type, typedBlocks) -> blockDetailRegistry.find(type).ifPresent(port ->
                putLoaded(byBlockId, typedBlocks, port)));

        return byBlockId;
    }

    private void putLoaded(Map<Long, BlockDetail> byBlockId, List<Block> typedBlocks,
                           BlockDetailPort port) {
        Map<Long, BlockDetail> loaded = port.loadDetails(
                typedBlocks.stream().map(Block::getTypeId).toList());

        for (Block block : typedBlocks) {
            BlockDetail detail = loaded.get(block.getTypeId());
            if (detail != null) {
                byBlockId.put(block.getBlockId(), detail);
            }
        }
    }

    private BlockSummary toSummary(Block block,
                                   Map<Long, BlockIssueStatLookupPort.BlockIssueStat> issueStats,
                                   Map<String, String> names,
                                   Map<Long, BlockDetail> details) {
        BlockIssueStatLookupPort.BlockIssueStat stat = issueStats.getOrDefault(
                block.getBlockId(), BlockIssueStatLookupPort.BlockIssueStat.empty());

        return new BlockSummary(
                block.getBlockId(), block.getType().name(), block.getTitle(),
                toOwner(block.getOwner(), names),
                block.getRowIndex(), block.getSortOrder(), block.getColSpan(),
                details.get(block.getBlockId()),
                stat.totalCount(), stat.doneCount());
    }

    /** 담당자를 안 지정했으면 null, 이름을 못 찾으면 사번만 담는다 (퇴사자로 조회가 비어도 응답이 깨지지 않게). */
    private BlockOwner toOwner(String userId, Map<String, String> names) {
        if (userId == null) {
            return null;
        }
        return new BlockOwner(userId, names.get(userId));
    }
}