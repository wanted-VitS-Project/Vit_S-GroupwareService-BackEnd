package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.block.application.command.CreateBlockCommand;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.result.BlockOwner;
import com.group3.vitamins.project.block.application.result.BlockResult;
import com.group3.vitamins.project.block.application.usecase.BlockCommandUseCase;
import com.group3.vitamins.project.block.domain.exception.BlockErrorCode;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BlockCommandService implements BlockCommandUseCase {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int MIN_COL_SPAN = 1;
    private static final int MAX_COL_SPAN = 3;
    private static final int DEFAULT_COL_SPAN = 1;
    private static final int FIRST_INDEX = 0;

    private final BlockRepository blockRepository;
    private final EmployeeLookupPort employeeLookupPort;
    private final BlockDetailRegistry blockDetailRegistry;
    private final StepAccessUseCase stepAccessUseCase;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public BlockResult createBlock(CreateBlockCommand command) {
        StepAccessUseCase.StepAccessView step = stepAccessUseCase.requireEditable(
                command.stepId(), command.requesterUserId(), command.role());

        BlockType type = resolveType(command.type());
        validateTitle(command.title());
        int colSpan = resolveColSpan(command.colSpan());
        checkSinglePerStep(command.stepId(), type);
        BlockOwner owner = resolveOwner(command.owner());

        int rowIndex = resolveRowIndex(command.stepId(), command.rowIndex());
        int sortOrder = resolveSortOrder(command.stepId(), rowIndex, command.sortOrder());

        LocalDateTime now = LocalDateTime.now();
        Block block = blockRepository.save(Block.create(
                command.stepId(), type, command.title(),
                owner == null ? null : owner.userId(),
                rowIndex, sortOrder, colSpan, command.requesterUserId(), now));

        linkDetail(block, type, now);
        publishBlockCreated(block, command.requesterUserId());

        return new BlockResult(
                block.getBlockId(), block.getStepId(), step.projectId(), type.name(),
                block.getTitle(), owner, block.getRowIndex(), block.getSortOrder(),
                block.getColSpan(), block.getCreatedAt());
    }

    /**
     * 상세 빈 행을 만들고 type_id 를 연결한다 (3단계 중 ②③).
     * 담당 어댑터가 없는 타입(FILE·PERFORMANCE_VIEW·TAX_INVOICE_VIEW)은 type_id 를 NULL 로 둔다.
     */
    private void linkDetail(Block block, BlockType type, LocalDateTime now) {
        Optional<BlockDetailPort> port = blockDetailRegistry.find(type);
        if (port.isEmpty()) {
            return;
        }

        Long typeId = port.get().createDetail(block.getBlockId());
        if (typeId == null) {
            throw new IllegalStateException(
                    "상세 행을 만들었는데 PK 를 찾지 못했다 - type=" + type + ", blockId=" + block.getBlockId());
        }

        block.linkTypeId(typeId, now);
        blockRepository.save(block);
    }

    private void publishBlockCreated(Block block, String actorId) {
        domainEventPublisher.publish(ActivityOccurredEvent.of(
                ActivityLogAction.CREATE,
                block.getBlockId(),
                null,
                actorId,
                List.of(new ActivityFieldChange(null, null, null))
        ));
    }

    /** 타입 문자열을 검증한다. enum 밖이거나 사용자가 만들 수 없는 타입이면 400 이다. */
    private BlockType resolveType(String rawType) {
        BlockType type = parseType(rawType);
        if (!type.userCreatable()) {
            throw new ValidationException(BlockErrorCode.BLOCK_TYPE_INVALID);
        }
        return type;
    }

    private BlockType parseType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            throw new ValidationException(BlockErrorCode.BLOCK_TYPE_INVALID);
        }
        try {
            return BlockType.valueOf(rawType);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(BlockErrorCode.BLOCK_TYPE_INVALID);
        }
    }

    /** 제목은 선택 입력이라 없어도 되고, 있으면 200자를 넘을 수 없다. */
    private void validateTitle(String title) {
        if (title != null && title.length() > TITLE_MAX_LENGTH) {
            throw new ValidationException(BlockErrorCode.BLOCK_TITLE_TOO_LONG);
        }
    }

    /** 미지정이면 1 칸이다. 총 열 수가 3 으로 고정이라 범위를 넘으면 400 이다. */
    private int resolveColSpan(Integer colSpan) {
        if (colSpan == null) {
            return DEFAULT_COL_SPAN;
        }
        if (colSpan < MIN_COL_SPAN || colSpan > MAX_COL_SPAN) {
            throw new ValidationException(BlockErrorCode.BLOCK_COL_SPAN_INVALID);
        }
        return colSpan;
    }

    /** 입금확인·세금계산서 조회는 스텝당 1개다. 둘이면 어느 회차 것인지 알 수 없어진다. */
    private void checkSinglePerStep(Long stepId, BlockType type) {
        if (!type.singlePerStep()) {
            return;
        }
        if (!blockRepository.existsByStepIdAndType(stepId, type)) {
            return;
        }
        throw new ConflictException(type == BlockType.PAYMENT_CONFIRM
                ? BlockErrorCode.PAYMENT_CONFIRM_BLOCK_DUPLICATED
                : BlockErrorCode.TAX_INVOICE_VIEW_BLOCK_DUPLICATED);
    }

    /** 담당자 사번을 보냈으면 존재를 확인하고 이름을 함께 돌려준다. 안 보냈으면 null. */
    private BlockOwner resolveOwner(String ownerUserId) {
        if (ownerUserId == null || ownerUserId.isBlank()) {
            return null;
        }
        String name = employeeLookupPort.findNameByUserId(ownerUserId);
        if (name == null) {
            throw new NotFoundException(ProjectErrorCode.USER_NOT_FOUND);
        }
        return new BlockOwner(ownerUserId, name);
    }

    /** 미지정이면 맨 아래 행(max+1). 블록이 없으면 0 행부터다. */
    private int resolveRowIndex(Long stepId, Integer rowIndex) {
        if (rowIndex != null) {
            return rowIndex;
        }
        return blockRepository.findMaxRowIndex(stepId)
                .map(max -> max + 1)
                .orElse(FIRST_INDEX);
    }

    /** 미지정이면 그 행 안의 max+1. 행이 비어 있으면 0 부터다. */
    private int resolveSortOrder(Long stepId, int rowIndex, Integer sortOrder) {
        if (sortOrder != null) {
            return sortOrder;
        }
        return blockRepository.findMaxSortOrder(stepId, rowIndex)
                .map(max -> max + 1)
                .orElse(FIRST_INDEX);
    }
}
