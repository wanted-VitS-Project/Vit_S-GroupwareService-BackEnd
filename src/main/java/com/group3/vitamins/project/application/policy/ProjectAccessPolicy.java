package com.group3.vitamins.project.application.policy;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class ProjectAccessPolicy {

    private static final Set<String> GLOBAL_ADMIN_ROLES = Set.of("MASTER", "ADMIN");

    /**
     * 프로젝트 접근 권한을 판정하고 요청자의 유효 권한을 돌려준다.
     * MASTER·ADMIN 은 참여자 행이 없어도 EDITOR 로 본다 (미참여 프로젝트 수정 시 privileged_override 기록 대상).
     */
    public MemberPermission resolvePermission(String role, MemberPermission memberPermission) {
        if (GLOBAL_ADMIN_ROLES.contains(role)) {
            return MemberPermission.EDITOR;
        }
        if (memberPermission == null || memberPermission == MemberPermission.NONE) {
            log.warn("프로젝트 접근 권한 없음 - role={}, permission={}", role, memberPermission);
            throw new ForbiddenException(ProjectErrorCode.PROJECT_ACCESS_DENIED);
        }
        return memberPermission;
    }

    /** 편집 권한을 요구한다. 참여자가 아니거나 VIEWER 면 거부한다 (스테이지·스텝 생성 등 쓰기 작업용). */
    public void requireEditor(String role, MemberPermission memberPermission) {
        if (GLOBAL_ADMIN_ROLES.contains(role)) {
            return;
        }
        if (memberPermission != MemberPermission.EDITOR) {
            log.warn("프로젝트 편집 권한 없음 - role={}, permission={}", role, memberPermission);
            throw new ForbiddenException(ProjectErrorCode.PROJECT_EDIT_DENIED);
        }
    }

    /**
     * 예외 없이 유효 권한만 판정한다. 참여자가 아니면 NONE 을 돌려준다.
     * 하위 애그리게이트가 자기 에러코드로 거부해야 할 때 쓴다 (스텝 상세는 403 을 STEP_ACCESS_DENIED 로 낸다).
     */
    public MemberPermission resolvePermissionOrNone(String role, MemberPermission memberPermission) {
        if (GLOBAL_ADMIN_ROLES.contains(role)) {
            return MemberPermission.EDITOR;
        }
        return memberPermission == null ? MemberPermission.NONE : memberPermission;
    }
}