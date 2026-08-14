package com.group3.vitamins.project.block.application.usecase;

import java.util.Map;

/**
 * 프로젝트 복제가 하위 블록을 복사하기 위해 쓰는 인바운드 유스케이스 (PRJ-018).
 *
 * <p>⚠️ <b>권한 검사를 하지 않는다</b> — 호출자(프로젝트 복제)가 원본 참여자 자격을 이미 확인한 뒤 부른다.
 * {@code BlockCascadeUseCase} 와 같은 계열이다.
 */
public interface BlockCloneUseCase {

    /** 프로젝트의 살아있는 블록 수. 복제 상한 판정용이라 블록을 읽지 않고 센다. */
    int countByProjectId(Long projectId);

    /**
     * 원본 스텝의 블록을 새 스텝으로 복사한다. 복사되는 것은 <b>골격뿐</b>이다 —
     * {@code type}·{@code title}·배치 3종만 옮기고 담당자는 비운다(참여자를 복제하지 않으므로
     * 담당자만 남기면 미참여자가 담당자가 된다).
     *
     * <p>상세 행은 <b>내용을 복사하지 않고</b> 생성 3단계 그대로 빈 행을 새로 만든다
     * (`BLOCK.md` §9-2 — 내용 복사는 v2).
     *
     * @param stepIdMap 원본 stepId → 새 stepId
     */
    BlockCloneCount cloneToSteps(Map<Long, Long> stepIdMap, String requesterUserId);

    /** {@code skipped} 는 사용자가 만들 수 없는 타입({@code BID_NOTICE})이라 건너뛴 수다. */
    record BlockCloneCount(int copied, int skipped) {
    }
}
