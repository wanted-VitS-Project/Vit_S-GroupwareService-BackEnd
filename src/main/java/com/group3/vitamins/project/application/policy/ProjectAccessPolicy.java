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
}