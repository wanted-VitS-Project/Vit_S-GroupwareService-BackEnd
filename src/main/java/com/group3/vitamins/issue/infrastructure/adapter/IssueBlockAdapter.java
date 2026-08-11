package com.group3.vitamins.issue.infrastructure.adapter;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.issue.application.port.IssueBlockPort;
import com.group3.vitamins.issue.application.port.IssueQueryPort;
import com.group3.vitamins.issue.domain.exception.IssueErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueBlockAdapter implements IssueBlockPort {

    private final IssueQueryPort issueQueryPort;

    @Override
    public List<BlockView> validateLinkable(Long stepId, List<Long> blockIds) {
        if (blockIds.isEmpty()) {
            return List.of();
        }

        List<IssueQueryPort.LinkableBlockResult> blocks = issueQueryPort.findLinkableBlocks(blockIds);
        if (blocks.size() != blockIds.size()) {
            throw new NotFoundException(IssueErrorCode.ISS_BLOCK_NOT_FOUND);
        }

        Map<Long, IssueQueryPort.LinkableBlockResult> byId = blocks.stream()
                .collect(Collectors.toMap(IssueQueryPort.LinkableBlockResult::blockId, block -> block));
        return blockIds.stream()
                .map(byId::get)
                .map(block -> {
                    if (!stepId.equals(block.stepId())) {
                        throw new ValidationException(IssueErrorCode.ISS_BLOCK_STEP_MISMATCH);
                    }
                    return new BlockView(block.blockId(), block.title(), block.type());
                })
                .toList();
    }
}
