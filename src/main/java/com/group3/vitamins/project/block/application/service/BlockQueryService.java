package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.port.BlockIssueStatLookupPort;
import com.group3.vitamins.project.block.application.query.BlockListQuery;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.BlockOption;
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

import java.util.*;
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

        //STEP 권한 검증
        stepAccessUseCase.requireAccess(query.stepId(), query.requesterUserId(), query.role());

        List<Block> blocks = blockRepository.findByStepId(query.stepId());
        if (blocks.isEmpty()) {
            return List.of();
        }

        Map<Long, BlockIssueStatLookupPort.BlockIssueStat> issueStats = blockIssueStatLookupPort
                .countByBlockIds(blocks.stream().map(Block::getBlockId).toList());


        Map<String, EmployeeLookupPort.EmployeeRef> names = employeeLookupPort.findRefsByUserIds(
                blocks.stream()
                        .map(Block::getOwner)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));


        Map<Long, BlockDetail> details = loadDetails(blocks);

        return blocks.stream()
                .map(block -> toSummary(block, issueStats, names, details))
                .toList();
    }

    /** 선택 후보 조회. 상세 어댑터·이슈 집계·담당자 조회를 하지 않아 쿼리 1개로 끝난다. */
    @Override
    public List<BlockOption> getBlockOptions(BlockListQuery query) {
        stepAccessUseCase.requireAccess(query.stepId(), query.requesterUserId(), query.role());

        return blockRepository.findByStepId(query.stepId()).stream()
                .map(block -> new BlockOption(
                        block.getBlockId(), block.getType().name(), block.getTitle()))
                .toList();
    }

    /**
     * 타입별로 typeId 를 모아 어댑터마다 한 번씩 배치 조회하고, 결과를 blockId 키로 바꿔 담는다.
     * 담당 어댑터가 없는 타입은 아무것도 담지 않아 응답에서 detail 이 null 이 된다.
     */
    private Map<Long, BlockDetail> loadDetails(Collection<Block> blocks) {
        Map<BlockType, List<Block>> byType = blocks.stream()
                .filter(block -> block.getTypeId() != null)
                .collect(Collectors.groupingBy(Block::getType));

        Map<Long, BlockDetail> byBlockId = new HashMap<>();
        for (Map.Entry<BlockType, List<Block>> entry : byType.entrySet()) {
            Optional<BlockDetailPort> port = blockDetailRegistry.find(entry.getKey());
            if (port.isEmpty()) {
                continue;
            }
            byBlockId.putAll(loadByType(entry.getValue(), port.get()));
        }
        return byBlockId;
    }

    /** 어댑터를 한 번 호출해 배치 조회하고, typeId 키를 blockId 키로 바꿔 돌려준다. */
    private Map<Long, BlockDetail> loadByType(List<Block> typedBlocks, BlockDetailPort port) {
        Map<Long, BlockDetail> byTypeId = port.loadDetails(
                typedBlocks.stream().map(Block::getTypeId).toList());

        Map<Long, BlockDetail> byBlockId = new HashMap<>();
        for (Block block : typedBlocks) {
            BlockDetail detail = byTypeId.get(block.getTypeId());
            if (detail != null) {
                byBlockId.put(block.getBlockId(), detail);
            }
        }
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
                                   Map<String, EmployeeLookupPort.EmployeeRef> names,
                                   Map<Long, BlockDetail> details) {
        BlockIssueStatLookupPort.BlockIssueStat stat = issueStats.getOrDefault(
                block.getBlockId(), BlockIssueStatLookupPort.BlockIssueStat.empty());

        return new BlockSummary(
                block.getBlockId(), block.getType().name(), block.getTitle(),
                toOwner(block.getOwner(), names),
                block.getRowIndex(), block.getSortOrder(), block.getColSpan(),
                details.get(block.getBlockId()),
                stat.totalCount(), stat.doneCount(),
                block.getVersion());
    }

    /**
     * 담당자를 안 지정했으면 null, 사번을 못 찾으면 사번만 담는다 (응답이 깨지지 않게).
     *
     * <p>삭제된 사원은 이름을 그대로 담고 {@code deleted = true} 로 알린다 — 지우면 화면에서
     * "담당자 없음" 과 구분이 안 된다 (DELETE.md D-6). 이름이 {@code null} 인 건
     * <b>사번이 employee 에 아예 없다</b>는 뜻이고, 그건 정합성 문제다.
     */
    private BlockOwner toOwner(String userId, Map<String, EmployeeLookupPort.EmployeeRef> names) {
        if (userId == null) {
            return null;
        }
        EmployeeLookupPort.EmployeeRef ref = names.get(userId);
        return ref == null
                ? new BlockOwner(userId, null, false)
                : new BlockOwner(userId, ref.name(), ref.deleted());
    }
}