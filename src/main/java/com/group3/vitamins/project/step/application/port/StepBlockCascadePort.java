package com.group3.vitamins.project.step.application.port;

import java.util.Collection;
import java.util.List;

/**
 * 스텝 삭제가 하위 블록을 옮기고 지우는 아웃바운드 포트 (STP-013 · BLK-014).
 * block 테이블을 직접 쓰지 않고 블록 애그리게이트의 인바운드 유스케이스에 위임한다.
 *
 * <p>⚠️ 요청자·역할을 넘기지 않는다. 스텝 삭제 권한(프로젝트 EDITOR)은 호출부에서 이미 판정했고,
 * 블록마다 스텝 EDITOR 를 다시 물으면 오버라이드 하나로 전체가 403 롤백된다.
 * {@code requesterUserId} 는 상세 정리에 기록용으로만 쓴다.
 */
public interface StepBlockCascadePort {

    /** 스텝의 미삭제 블록 ID 전부. */
    List<Long> findBlockIds(Long stepId);

    /** 지정한 블록을 다른 스텝으로 옮긴다. issue_block 연결은 끊긴다. */
    void moveBlocks(Collection<Long> blockIds, Long toStepId);

    /** 지정한 블록을 논리 삭제한다. 타입별 상세도 함께 지워진다. */
    void deleteBlocks(Collection<Long> blockIds, String requesterUserId);
}
