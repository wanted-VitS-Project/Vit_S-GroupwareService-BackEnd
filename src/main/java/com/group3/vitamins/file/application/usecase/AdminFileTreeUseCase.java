package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.result.AdminTreeProjectPageResult;
import com.group3.vitamins.file.application.result.AdminTreeStageProjection;
import com.group3.vitamins.file.application.result.AdminTreeStepProjection;
import com.group3.vitamins.file.application.result.CompanyFilePageResult;

import java.util.List;

/**
 * 전사 파일 트리 탐색(§14 · ADMIN). 프로젝트 → 스테이지 → 스텝 → 파일 lazy 조회.
 *
 * <p>전 메서드가 ADMIN 을 명시 검사하고 회사(테넌트) 스코프로만 조회한다(FILE-Q-01 과 동일 정책).
 */
public interface AdminFileTreeUseCase {

    /** §14.1 회사 프로젝트 한 페이지. */
    AdminTreeProjectPageResult getProjects(String role, int page, int size);

    /** §14.2 프로젝트의 스테이지(+ 미분류 버킷). 프로젝트가 없으면 PROJECT_NOT_FOUND. */
    List<AdminTreeStageProjection> getStages(String role, Long projectId);

    /** §14.3 프로젝트의 스텝. stageId 가 null 이면 미분류. 프로젝트가 없으면 PROJECT_NOT_FOUND. */
    List<AdminTreeStepProjection> getSteps(String role, Long projectId, Long stageId);

    /** §14.4 스텝의 파일 한 페이지. 스텝이 없으면 FILE_STEP_NOT_FOUND. */
    CompanyFilePageResult getStepFiles(String role, Long stepId, int page, int size);
}
