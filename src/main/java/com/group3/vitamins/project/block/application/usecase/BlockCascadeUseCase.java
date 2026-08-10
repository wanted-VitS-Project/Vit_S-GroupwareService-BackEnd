package com.group3.vitamins.project.block.application.usecase;

import java.util.Collection;
import java.util.List;

/**
 * 스텝 삭제가 하위 블록을 훑고 정리하기 위해 쓰는 인바운드 유스케이스 (STP-013).
 *
 * <p>⚠️ <b>권한 검사를 하지 않는다</b> — 호출자(스텝 삭제)가 이미 프로젝트 EDITOR 를 확인한 뒤 부른다.
 * {@link BlockCommandUseCase} 를 그대로 쓰면 블록마다 <b>스텝</b> EDITOR 를 다시 요구해서,
 * 요청자가 그 스텝에 NONE·VIEWER 오버라이드를 갖고 있으면 명세상 허용된 삭제가 403 으로 롤백된다.
 */
public interface BlockCascadeUseCase {

    /** 스텝의 미삭제 블록 ID 전부. rowIndex → sortOrder 순. */
    List<Long> findBlockIds(Long stepId);

    /**
     * 지정한 블록을 다른 스텝으로 옮긴다. {@code issue_block} 연결은 끊긴다 (BLK-014).
     * 이동 대상이 같은 프로젝트인지는 <b>호출자가 검증한다</b>.
     */
    void moveBlocks(Collection<Long> blockIds, Long toStepId);

    /** 지정한 블록을 논리 삭제한다. 타입별 상세도 함께 지워진다. */
    void deleteBlocks(Collection<Long> blockIds, String requesterUserId);
}
