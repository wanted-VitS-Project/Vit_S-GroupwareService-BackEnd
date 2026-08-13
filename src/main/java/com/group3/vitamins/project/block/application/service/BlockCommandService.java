package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.block.application.command.CreateBlockCommand;
import com.group3.vitamins.project.block.application.command.DeleteBlockCommand;
import com.group3.vitamins.project.block.application.command.MoveBlockCommand;
import com.group3.vitamins.project.block.application.command.UpdateBlockCommand;
import com.group3.vitamins.project.block.application.command.UpdateBlockLayoutCommand;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.port.IssueBlockUnlinkPort;
import com.group3.vitamins.project.block.application.result.BlockLayoutResult;
import com.group3.vitamins.project.block.application.result.BlockMoveResult;
import com.group3.vitamins.project.block.application.result.BlockOwner;
import com.group3.vitamins.project.block.application.result.BlockResult;
import com.group3.vitamins.project.block.application.result.BlockUpdateResult;
import com.group3.vitamins.project.block.application.usecase.BlockCascadeUseCase;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BlockCommandService implements BlockCommandUseCase, BlockCascadeUseCase {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int MIN_COL_SPAN = 1;
    private static final int MAX_COL_SPAN = 3;
    private static final int DEFAULT_COL_SPAN = 1;
    private static final int FIRST_INDEX = 0;

    private final BlockRepository blockRepository;
    private final EmployeeLookupPort employeeLookupPort;
    private final BlockDetailRegistry blockDetailRegistry;
    private final IssueBlockUnlinkPort issueBlockUnlinkPort;
    private final StepAccessUseCase stepAccessUseCase;

    @Override
    public BlockResult createBlock(CreateBlockCommand command) {

        //권한 검증
        StepAccessUseCase.StepAccessView step = stepAccessUseCase.requireEditable(
                command.stepId(), command.requesterUserId(), command.role());

        //값 검증
        BlockType type = resolveType(command.type());
        validateTitle(command.title());
        checkSinglePerStep(command.stepId(), type);

        //열 검증
        int colSpan = resolveColSpan(command.colSpan());
        int rowIndex = resolveRowIndex(command.stepId(), command.rowIndex());
        int sortOrder = resolveSortOrder(command.stepId(), rowIndex, command.sortOrder());

        //사번 검증, 이름 삽입
        BlockOwner owner = resolveOwner(command.owner());

        //만든 시간 삽입
        LocalDateTime now = LocalDateTime.now();

        //1. 블록 TABLE 생성, BLOCK PK 생성
        Block block = blockRepository.save(Block.create(
                command.stepId(), type, command.title(),
                owner == null ? null : owner.userId(),
                rowIndex, sortOrder, colSpan, command.requesterUserId(), now));

        //2. 1)상세 테이블 조회, 2)행 추가, 3)TYPE ID 조회, 4) TYPE ID 삽입
        linkDetail(block, type, now);

        return new BlockResult(
                block.getBlockId(), block.getStepId(), step.projectId(), type.name(),
                block.getTitle(), owner, block.getRowIndex(), block.getSortOrder(),
                block.getColSpan(), block.getCreatedAt());
    }

    /**
     * 블록 고유값(제목·담당자)만 바꾼다. 배치는 updateLayout, 타입은 생성 후 변경 불가다
     * (상세 테이블이 달라진다).
     *
     * <p><b>내가 조회한 버전이 아직 최신일 때만</b> 저장된다 (`.ai/docs/global/CONCURRENCY.md`).
     * ⚠️ {@code save()} 로 되돌리지 마라 — 검사와 저장이 한 문장이 아니면 그 틈에 남의 저장이 끼어들어
     * <b>예외도 로그도 없이</b> 갱신이 유실된다 (§6-4).
     */
    @Override
    public BlockUpdateResult updateBlock(UpdateBlockCommand command) {

        //404 가 403 보다 먼저다 — 블록을 찾아야 stepId 를 알고 권한을 물을 수 있다
        Block block = findBlock(command.blockId());
        stepAccessUseCase.requireEditable(
                block.getStepId(), command.requesterUserId(), command.role());

        if (!command.titleProvided() && !command.ownerProvided()) {
            throw new ValidationException(BlockErrorCode.BLOCK_UPDATE_FIELD_REQUIRED);
        }

        int expected = command.overwrite() ? block.getVersion() : command.version();
        LocalDateTime now = LocalDateTime.now();

        if (command.titleProvided()) {
            validateTitle(command.title());
            block.changeTitle(command.title(), now);
        }

        BlockOwner owner = null;
        if (command.ownerProvided()) {
            //null·공백을 보내면 해제, 없는 사번이면 404
            owner = resolveOwner(command.owner());
            block.changeOwner(owner == null ? null : owner.userId(), now);
        }

        // ⚠️ 요청값이 아니라 도메인이 반영을 끝낸 최종값을 넘긴다. UPDATE 는 두 컬럼을 모두 SET 하므로
        //    생략한 필드에 command.title() 을 그대로 넘기면 건드리지도 않은 값이 null 로 지워진다.
        int updated = blockRepository.updateIfVersionMatches(
                block.getBlockId(), block.getTitle(), block.getOwner(), now, expected);

        if (updated == 0) {
            throw new ConflictException(BlockErrorCode.BLOCK_VERSION_CONFLICT);
        }

        //담당자를 안 건드렸으면 기존 담당자를 그대로 내려야 한다 — 이름은 관대하게 조회한다
        return new BlockUpdateResult(block.getBlockId(), block.getTitle(),
                command.ownerProvided() ? owner : describeOwner(block.getOwner()),
                now, expected + 1);
    }

    /**
     * 드래그 결과를 한 트랜잭션에서 일괄 반영한다. 같은 행에 순서가 겹쳐도 허용한다 (BLK-004) —
     * UNIQUE 를 걸지 않은 이유가 드래그 중간 상태다.
     */
    @Override
    public List<BlockLayoutResult> updateLayout(UpdateBlockLayoutCommand command) {

        //stepId 가 경로에 있어 여기선 권한이 먼저다
        stepAccessUseCase.requireEditable(
                command.stepId(), command.requesterUserId(), command.role());

        List<UpdateBlockLayoutCommand.BlockLayout> layouts = command.layouts();
        if (layouts == null || layouts.isEmpty()) {
            throw new ValidationException(BlockErrorCode.BLOCK_LAYOUT_INVALID);
        }
        layouts.forEach(this::validateLayout);

        List<Long> blockIds = layouts.stream()
                .map(UpdateBlockLayoutCommand.BlockLayout::blockId)
                .toList();

        //중복 blockId 를 안 막으면 아래 개수 비교가 404 로 새는데, 실제 원인은 요청이 모순인 것이다
        if (new HashSet<>(blockIds).size() != blockIds.size()) {
            throw new ValidationException(BlockErrorCode.BLOCK_LAYOUT_INVALID);
        }

        Map<Long, Block> found = blockRepository.findAllByIds(blockIds).stream()
                .collect(Collectors.toMap(Block::getBlockId, Function.identity()));

        if (found.size() != blockIds.size()) {
            throw new NotFoundException(BlockErrorCode.BLOCK_NOT_FOUND);
        }
        //남의 스텝 블록을 섞어 보내면 그 블록이 화면에서 사라진다
        boolean otherStep = found.values().stream()
                .anyMatch(block -> !block.getStepId().equals(command.stepId()));
        if (otherStep) {
            throw new ValidationException(BlockErrorCode.BLOCK_LAYOUT_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        List<BlockLayoutResult> results = new ArrayList<>(layouts.size());

        for (UpdateBlockLayoutCommand.BlockLayout layout : layouts) {
            Block block = relocate(found.get(layout.blockId()), layout, now);

            int updated = blockRepository.relocateIfVersionMatches(
                    block.getBlockId(), block.getRowIndex(), block.getSortOrder(),
                    block.getColSpan(), now, layout.version());

            // ⚠️ 이 예외를 잡아서 넘기면 앞 항목만 커밋되어 그리드가 반쯤 바뀐 채 남는다.
            //    ConflictException 은 런타임 예외라 여기서 던져야 트랜잭션 전체가 롤백된다 (§4-3).
            if (updated == 0) {
                throw new ConflictException(BlockErrorCode.BLOCK_VERSION_CONFLICT);
            }

            results.add(new BlockLayoutResult(block.getBlockId(), block.getRowIndex(),
                    block.getSortOrder(), block.getColSpan(), layout.version() + 1));
        }

        return results;
    }

    /**
     * 같은 프로젝트의 다른 스텝으로 옮긴다 (BLK-014).
     *
     * <p>⚠️ 권한을 <b>양쪽 다</b> 확인한다. 출발 스텝만 보면 접근 권한이 없는 스텝으로 블록을 밀어넣을 수 있다.
     * 그 판정 결과에서 두 스텝의 projectId 를 얻어 같은 프로젝트인지도 함께 본다 —
     * Block 은 projectId 를 갖지 않아 이 경로 말고는 알 방법이 없다.
     *
     * <p>issue_block 연결은 끊는다. 블록과 이슈는 같은 스텝이어야 하는데(BLK-009 · INV-06)
     * 옮기면 그 전제가 깨진다.
     */
    @Override
    public BlockMoveResult moveBlock(MoveBlockCommand command) {

        if (command.stepId() == null) {
            throw new ValidationException(BlockErrorCode.BLOCK_MOVE_TARGET_REQUIRED);
        }

        Block block = findBlock(command.blockId());

        StepAccessUseCase.StepAccessView from = stepAccessUseCase.requireEditable(
                block.getStepId(), command.requesterUserId(), command.role());
        StepAccessUseCase.StepAccessView to = stepAccessUseCase.requireEditable(
                command.stepId(), command.requesterUserId(), command.role());

        if (from.stepId().equals(to.stepId()) || !from.projectId().equals(to.projectId())) {
            throw new ValidationException(BlockErrorCode.BLOCK_MOVE_TARGET_INVALID);
        }

        int expected = command.overwrite() ? block.getVersion() : command.version();
        int unlinkedIssueCount = issueBlockUnlinkPort.unlinkByBlockId(block.getBlockId());

        LocalDateTime now = LocalDateTime.now();
        int rowIndex = nextRowIndex(to.stepId());
        block.moveToStep(to.stepId(), rowIndex, FIRST_INDEX, now);

        int updated = blockRepository.moveToStepIfVersionMatches(
                block.getBlockId(), to.stepId(), rowIndex, FIRST_INDEX, now, expected);

        if (updated == 0) {
            throw new ConflictException(BlockErrorCode.BLOCK_VERSION_CONFLICT);
        }

        return new BlockMoveResult(
                block.getBlockId(), block.getStepId(), unlinkedIssueCount, expected + 1);
    }

    /**
     * cascade 이동 본체 (스텝 삭제가 부른다). ⛔ 여기엔 낙관락을 걸지 않는다 —
     * 사용자가 버전을 들고 있는 요청이 아니라 스텝 삭제가 연쇄로 옮기는 경로다
     * (`.ai/docs/global/CONCURRENCY.md`). 사용자 이동은 {@link #moveBlock} 이 전담한다.
     */
    private int moveToStep(Block block, Long toStepId) {
        int unlinkedIssueCount = issueBlockUnlinkPort.unlinkByBlockId(block.getBlockId());

        block.moveToStep(toStepId, nextRowIndex(toStepId), FIRST_INDEX, LocalDateTime.now());
        blockRepository.save(block);
        return unlinkedIssueCount;
    }

    /** 도착 스텝의 맨 아래 새 행에 붙인다. 원래 좌표를 들고 가면 기존 블록과 겹쳐 그리드가 깨진다. */
    private int nextRowIndex(Long stepId) {
        return blockRepository.findMaxRowIndex(stepId)
                .map(max -> max + 1)
                .orElse(FIRST_INDEX);
    }

    /**
     * 블록과 상세 행을 같은 트랜잭션에서 논리 삭제한다. 하드 삭제는 없다 (INV-05).
     *
     * <p>⛔ 삭제 잠금 4종은 폐기됐다 (2026-08-09 · BLK-008). 막아도 탈출구가 재무팀 호출뿐이라
     * 사용자가 갇혔다 — 막는 대신 블록을 옮길 수단을 준다 (BLK-014).
     * 재무 연결 해제(BLK-013)는 재무 접근 경로와 함께 별도로 붙는다 — 대상은
     * {@code cash_flow.settle_block_id}·{@code tax_invoice.settle_block_id} 다.
     *
     * <p>⛔ 복구(휴지통)도 없다. 되돌리고 싶으면 삭제 전에 옮긴다 (BLK-014) — `PRJ-V1.md` §3-1.
     *
     * <p>📌 <b>상신된 결재는 「확인 요구」다 — 잠금이 아니다 (2026-08-12 · DEL-016).</b> 첫 요청을 409 로
     * 되묻고 {@code confirmApprovalCancel=true} 재요청이면 삭제한다. 즉 <b>영구히 막지 않는다.</b>
     * 스텝 삭제 cascade({@link #deleteBlocks})는 판정을 부르지 않아 결재 때문에 롤백되지도 않는다.
     * 판정 기준·문구는 결재 도메인이 갖는다 ({@code APR-DELETE-DRAFT.md} §11).
     */
    @Override
    public void deleteBlock(DeleteBlockCommand command) {

        Block block = findBlock(command.blockId());
        stepAccessUseCase.requireEditable(
                block.getStepId(), command.requesterUserId(), command.role());

        // ⚠️ 판정은 이 경로에만 있다. 아래 deleteBlock(block, ...) 본체는 cascade 와 공유하므로
        //    거기로 옮기면 스텝 삭제도 함께 막혀 409 롤백된다 (DEL-017 · BLK-008 폐기 사유).
        assertDetailDeletable(block, command.confirmApprovalCancel());

        deleteBlock(block, command.requesterUserId());

        // 블록 삭제 활동 로그(§5.1)는 여기서 발행해야 하지만 활동기록 공통 컴포넌트(#43)가
        // 아직 없다. 타입별 도메인에서 발행하면 어댑터 없는 타입(FILE·APPROVAL)이 영구 누락된다.
    }

    /** 삭제 본체. 권한 판정이 끝난 뒤의 처리라 cascade 경로와 공유한다. */
    private void deleteBlock(Block block, String requesterUserId) {
        LocalDateTime now = LocalDateTime.now();

        // ⚠️ 상세를 먼저 지운다. 순서를 뒤집으면 block.deleted_at 이 유실된다 —
        // 타입 도메인의 삭제가 @Modifying(clearAutomatically = true) 벌크 UPDATE 인데
        // flushAutomatically 가 기본값(false) 이라, 앞서 save() 한 block 의 pending UPDATE 를
        // flush 하지 않고 영속성 컨텍스트를 비워버린다. 커밋해도 block 은 안 지워지고
        // 상세만 지워져 조회에는 그대로 뜨는 상태가 된다 (2026-08-05 런타임 검증으로 발견).
        // 같은 트랜잭션이라 중간 상태는 밖에서 보이지 않는다 — BLK-007(판정 주인은 block.deleted_at)은
        // 읽기 규칙이고 쓰기 순서 규칙이 아니다.
        unlinkDetail(block, requesterUserId, now);

        block.delete(now);
        blockRepository.save(block);
    }

    /**
     * 스텝 삭제가 부르는 블록 정리 (STP-013). 권한은 호출자가 프로젝트 EDITOR 로 이미 판정했다 —
     * 여기서 스텝 EDITOR 를 다시 보면 오버라이드 하나로 삭제 전체가 403 롤백된다.
     */
    @Override
    public List<Long> findBlockIds(Long stepId) {
        return blockRepository.findByStepId(stepId).stream()
                .map(Block::getBlockId)
                .toList();
    }

    @Override
    public void moveBlocks(Collection<Long> blockIds, Long toStepId) {
        findBlocks(blockIds).forEach(block -> moveToStep(block, toStepId));
    }

    @Override
    public void deleteBlocks(Collection<Long> blockIds, String requesterUserId) {
        findBlocks(blockIds).forEach(block -> deleteBlock(block, requesterUserId));
    }

    /**
     * cascade 대상 블록을 한 번에 읽는다. 건마다 {@code findById} 를 돌리면 스텝 삭제 한 번이
     * 블록 수만큼 SELECT 를 낸다.
     *
     * <p>⚠️ 받은 순서를 그대로 유지한다 — {@code findAllByIds} 의 반환 순서는 보장이 없는데
     * {@code moveToStep} 이 도착 스텝의 <b>맨 아래 행</b>에 차례로 붙이므로, 순서가 흔들리면
     * 옮겨진 블록들의 위아래가 요청과 달라진다. 예외도 로그도 없이 배치만 바뀐다.
     *
     * <p>이미 삭제된 id 는 결과에서 빠져 조용히 통과한다 — 삭제는 멱등해야 한다
     * ({@code .ai/docs/global/DELETE.md} §3 레이어 2). 상세 삭제가 404 로 막히면 스텝 삭제 전체가 롤백된다.
     */
    private List<Block> findBlocks(Collection<Long> blockIds) {
        if (blockIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Block> byId = blockRepository.findAllByIds(blockIds).stream()
                .collect(Collectors.toMap(Block::getBlockId, Function.identity()));

        return blockIds.stream()
                .map(byId::get)
                .filter(block -> block != null)
                .toList();
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

    /**
     * 상세 타입이 지금 삭제를 허용하는지 묻는다 (DEL-016). 확인이 필요하면 어댑터가 409 를 던진다.
     *
     * <p>상태 개념이 없는 타입은 포트 기본 구현이 no-op 이라 그대로 통과한다. 현재 되묻는 타입은
     * APPROVAL 하나이며, 블록 도메인은 그 판정 기준을 알지 않는다 (DEL-018).
     *
     * <p>⚠️ 확인 플래그가 <b>모든 타입에 함께</b> 전달된다. 지금은 되묻는 타입이 하나라 문제가 없지만,
     * 두 번째 타입(예: 재무 연결 해제 — {@code BLOCK.md} §8)이 생기면 플래그를 타입별로 나눠야 한다.
     * 그때 이 파라미터를 값 객체로 바꾼다 — 하나의 확인이 다른 타입의 확인을 대신하면 안 된다.
     */
    private void assertDetailDeletable(Block block, boolean confirmed) {
        if (block.getTypeId() == null) {
            return;
        }
        blockDetailRegistry.find(block.getType())
                .ifPresent(port -> port.assertDeletable(block.getTypeId(), block.getTitle(), confirmed));
    }

    /**
     * 상세 행도 같은 트랜잭션에서 논리 삭제한다. ⛔ 이벤트로 넘기지 않는다 — 상세는 block_id NOT NULL 이라
     * 독립 생명주기가 없고, 유실되면 회수할 주체가 없다 (결과적 일관성이 아니라 그냥 유실이다).
     * 어댑터가 없거나 상세 PK 가 없는 타입은 지울 것도 없다.
     */
    private void unlinkDetail(Block block, String requesterUserId, LocalDateTime now) {
        if (block.getTypeId() == null) {
            return;
        }
        blockDetailRegistry.find(block.getType())
                .ifPresent(port -> port.deleteDetail(
                        block.getTypeId(), requesterUserId, block.getTitle(), now));
    }

    private Block findBlock(Long blockId) {
        return blockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException(BlockErrorCode.BLOCK_NOT_FOUND));
    }

    private Block relocate(Block block, UpdateBlockLayoutCommand.BlockLayout layout,
                           LocalDateTime now) {
        block.relocate(layout.rowIndex(), layout.sortOrder(), layout.colSpan(), now);
        return block;
    }

    /**
     * 배치 3필드는 전부 필수다. 누락을 통과시키면 블록이 조용히 0행 0열로 옮겨진다.
     * version 누락은 전용 코드로 알린다 — 0 을 채워 보내면 저장이 전부 409 가 되는데
     * 사용자에게는 원인이 안 보인다 (§6-3).
     */
    private void validateLayout(UpdateBlockLayoutCommand.BlockLayout layout) {
        if (layout.blockId() == null || layout.rowIndex() == null
                || layout.sortOrder() == null || layout.colSpan() == null) {
            throw new ValidationException(BlockErrorCode.BLOCK_LAYOUT_INVALID);
        }
        if (layout.version() == null) {
            throw new ValidationException(BlockErrorCode.BLOCK_VERSION_REQUIRED);
        }
        validateColSpan(layout.colSpan());
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

    /** 미지정이면 1 칸이다. 생성 전용 — 배치 변경은 필수라 validateColSpan 을 직접 쓴다. */
    private int resolveColSpan(Integer colSpan) {
        if (colSpan == null) {
            return DEFAULT_COL_SPAN;
        }
        validateColSpan(colSpan);
        return colSpan;
    }

    /** 총 열 수가 3 으로 고정이라 범위를 넘으면 400 이다 (BLK-003). */
    private void validateColSpan(int colSpan) {
        if (colSpan < MIN_COL_SPAN || colSpan > MAX_COL_SPAN) {
            throw new ValidationException(BlockErrorCode.BLOCK_COL_SPAN_INVALID);
        }
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
        // 위에서 name == null 을 막았고 findNameByUserId 가 deleted_at IS NULL 을 보므로 삭제된 사원은 못 온다.
        return new BlockOwner(ownerUserId, name, false);
    }

    /**
     * 응답에 담을 기존 담당자. 이름을 못 찾아도 사번만 담는다 — 퇴사자로 조회가 비었다고
     * 제목 수정이 404 로 실패하면 안 된다 (블록 조회 API 와 같은 규칙).
     */
    private BlockOwner describeOwner(String ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        // 쓰기 경로다 — findNameByUserId 가 deleted_at IS NULL 을 검증하므로 삭제된 사원은 여기 못 온다.
        return new BlockOwner(ownerUserId, employeeLookupPort.findNameByUserId(ownerUserId), false);
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