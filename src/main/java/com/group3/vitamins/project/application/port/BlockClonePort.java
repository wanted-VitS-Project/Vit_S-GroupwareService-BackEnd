package com.group3.vitamins.project.application.port;

import java.util.Map;

/**
 * 프로젝트 복제 시 블록 애그리게이트에 복사를 요청하는 아웃바운드 포트 (PRJ-018).
 */
public interface BlockClonePort {

    /** 프로젝트의 살아있는 블록 수. 복제 상한 판정용이라 복사를 시작하기 전에 부른다. */
    int countBlocks(Long projectId);

    /**
     * 블록 골격을 새 스텝으로 복사한다. 상세는 빈 행으로 새로 만들어진다 (내용 복사는 v2).
     *
     * @param stepIdMap {@link StepClonePort#cloneSteps} 가 돌려준 매핑
     */
    ClonedBlocks cloneBlocks(Map<Long, Long> stepIdMap, String requesterUserId);

    /** {@code skipped} 는 사용자가 만들 수 없는 타입({@code BID_NOTICE})이라 건너뛴 수다. */
    record ClonedBlocks(int copied, int skipped) {
    }
}
