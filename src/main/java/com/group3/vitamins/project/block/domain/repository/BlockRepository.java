package com.group3.vitamins.project.block.domain.repository;

import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;

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
}