package com.group3.vitamins.approval.application.result;

/** 결재 상세조회(MGT-006) — 원본 블록·스텝·프로젝트 이동 정보. */
public record BlockOriginView(Long blockId, Long stepId, Long projectId) {
}
