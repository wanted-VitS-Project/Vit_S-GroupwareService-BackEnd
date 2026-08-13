package com.group3.vitamins.project.block.domain.repository;

import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BlockRepository {

    /** 생성·수정·삭제 공용 저장. */
    Block save(Block block);

    /** 배치 변경에서 여러 블록을 한 번에 저장한다. */
    List<Block> saveAll(List<Block> blocks);

    /** 논리 삭제분은 조회하지 않는다. 수정·삭제·이슈 연결의 404 판정에 쓴다. */
    Optional<Block> findById(Long blockId);

    /** rowIndex → sortOrder 오름차순 목록. 블록 일괄 조회에 쓴다. */
    List<Block> findByStepId(Long stepId);

    /**
     * 여러 스텝의 블록을 한 번에 읽는다 (프로젝트 복제 · PRJ-018).
     * {@code stepId → rowIndex → sortOrder} 오름차순이라 스텝별로 잘라 쓰면 배치 순서가 유지된다.
     */
    List<Block> findByStepIds(Collection<Long> stepIds);

    /**
     * 프로젝트의 살아있는 블록 수. 복제 상한(300) 판정에 쓴다 — 세려고 전부 읽지 않는다.
     *
     * <p>{@code block} 은 {@code project_id} 를 갖지 않으므로 {@code step} 을 조인해서 센다
     * (`BLOCK.md` §1 — <i>"프로젝트를 알아야 하면 step 을 조인한다"</i>).
     */
    int countByProjectId(Long projectId);

    /** 배치 변경에서 요청에 담긴 블록을 한 번에 읽는다. 개수가 다르면 404 판정. */
    List<Block> findAllByIds(Collection<Long> blockIds);

    /** 생성 시 rowIndex 미지정이면 이 값 + 1 을 쓴다. 블록이 없으면 empty. */
    Optional<Integer> findMaxRowIndex(Long stepId);

    /** 생성 시 sortOrder 미지정이면 그 행 안의 이 값 + 1 을 쓴다. 행이 비면 empty. */
    Optional<Integer> findMaxSortOrder(Long stepId, int rowIndex);

    /** 스텝당 1개 타입(PAYMENT_CONFIRM·TAX_INVOICE_VIEW) 의 409 판정. */
    boolean existsByStepIdAndType(Long stepId, BlockType type);

    /**
     * 타입 + 상세 PK 로 역방향 조회. 타입별 도메인이 상세 PK 만 갖고 권한을 물어올 때 쓴다
     * (BlockCatalogPort 구현). idx_block_type_id 가 이 조회용이다.
     */
    Optional<Block> findByTypeAndTypeId(BlockType type, Long typeId);

    /**
     * 기대 버전과 DB 버전이 같을 때만 제목·담당자를 덮어쓰고 version 을 올린다.
     * 바뀐 행 수를 돌려준다 — <b>0 이면 그 사이 남이 먼저 저장한 것이다(충돌)</b>.
     *
     * <p>⚠️ {@code save()} 로 대체하지 마라. 검사와 저장이 한 문장 안에서 원자적으로 일어나야
     * 조회~저장 사이의 갱신 유실을 막는다 (`.ai/docs/global/CONCURRENCY.md` §1-3 · §6-4).
     */
    int updateIfVersionMatches(Long blockId, String title, String owner,
                               LocalDateTime updatedAt, int expectedVersion);

    /** 기대 버전이 같을 때만 배치를 옮긴다. 0 이면 충돌이다. */
    int relocateIfVersionMatches(Long blockId, int rowIndex, int sortOrder, int colSpan,
                                 LocalDateTime updatedAt, int expectedVersion);

    /** 기대 버전이 같을 때만 다른 스텝으로 옮긴다. 0 이면 충돌이다. */
    int moveToStepIfVersionMatches(Long blockId, Long stepId, int rowIndex, int sortOrder,
                                   LocalDateTime updatedAt, int expectedVersion);
}