package com.group3.vitamins.department.application.usecase;

import com.group3.vitamins.department.application.result.DepartmentTreeResult;

import java.util.List;

/**
 * 부서 조회 인바운드 포트 (`.ai/api/department.md` §1). 권한은 전체 사용자(인증만).
 */
public interface DepartmentQueryUseCase {

    /** 전체 부서를 최대 2단 트리로 조립해 최상위 목록으로 반환한다. */
    List<DepartmentTreeResult> getDepartmentTree();
}
