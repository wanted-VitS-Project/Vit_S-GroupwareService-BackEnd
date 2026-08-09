package com.group3.vitamins.project.block.infrastructure.adapter;

import com.group3.vitamins.project.block.application.port.IssueBlockUnlinkPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueBlockUnlinkAdapter implements IssueBlockUnlinkPort {

    private final IssueBlockUnlinkMapper issueBlockUnlinkMapper;

    @Override
    public int unlinkByBlockId(Long blockId) {
        return issueBlockUnlinkMapper.deleteByBlockId(blockId);
    }
}
