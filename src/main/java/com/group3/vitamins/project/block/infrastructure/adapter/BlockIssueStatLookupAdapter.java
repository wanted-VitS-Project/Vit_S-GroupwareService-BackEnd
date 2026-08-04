package com.group3.vitamins.project.block.infrastructure.adapter;

import com.group3.vitamins.project.block.application.port.BlockIssueStatLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BlockIssueStatLookupAdapter implements BlockIssueStatLookupPort {

    private final BlockIssueStatQueryMapper blockIssueStatQueryMapper;

    @Override
    public Map<Long, BlockIssueStat> countByBlockIds(Collection<Long> blockIds) {
        if (blockIds.isEmpty()) {
            return Map.of();
        }
        return blockIssueStatQueryMapper.countByBlockIds(blockIds).stream()
                .collect(Collectors.toMap(BlockIssueStatRow::blockId,
                        row -> new BlockIssueStat(row.totalCount(), row.doneCount())));
    }
}