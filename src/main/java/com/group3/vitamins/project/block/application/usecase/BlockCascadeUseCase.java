package com.group3.vitamins.project.block.application.usecase;

import java.util.List;

/**
 * 스텝 삭제가 하위 블록을 훑기 위해 쓰는 인바운드 유스케이스 (STP-013).
 *
 * <p>권한 검사를 하지 않는다 — 호출자(스텝 삭제)가 이미 프로젝트 EDITOR 를 확인한 뒤 부른다.
 * 실제 이동·삭제는 {@link BlockCommandUseCase} 를 그대로 쓴다.
 */
public interface BlockCascadeUseCase {

    /** 스텝의 미삭제 블록 ID 전부. rowIndex → sortOrder 순. */
    List<Long> findBlockIds(Long stepId);
}
