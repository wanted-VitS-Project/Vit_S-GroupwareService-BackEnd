package com.group3.vitamins.project.step.application.policy;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class StepAccessPolicy {

    private static final Set<String> GLOBAL_ADMIN_ROLES = Set.of("MASTER", "ADMIN");

    /**
     * 스텝 권한을 판정한다. 프로젝트 권한이 없으면 오버라이드를 보지 않고,
     * 있으면 오버라이드가 우선하며 없을 때 프로젝트 권한을 상속한다.
     * MASTER·ADMIN 은 오버라이드까지 무시하고 EDITOR 로 본다 — 프로젝트 권한 판정과 같은 기준이다.
     */
    public MemberPermission resolve(String role, MemberPermission projectPermission,
                                    MemberPermission override) {
        if (GLOBAL_ADMIN_ROLES.contains(role)) {
            return MemberPermission.EDITOR;
        }
        if (projectPermission == null || projectPermission == MemberPermission.NONE) {
            return MemberPermission.NONE;
        }
        return override != null ? override : projectPermission;
    }

    /** NONE 이면 스텝을 볼 수 없다 (STP-010) — 목록에서는 제외하고 상세에서는 403 이다. */
    public boolean isAccessible(MemberPermission permission) {
        return permission != MemberPermission.NONE;
    }

    /** 상세 조회용. 접근 권한이 없으면 STEP_ACCESS_DENIED 로 거부한다. */
    public MemberPermission requireAccess(String role, MemberPermission projectPermission,
                                          MemberPermission override) {
        MemberPermission permission = resolve(role, projectPermission, override);
        if (!isAccessible(permission)) {
            log.warn("스텝 접근 권한 없음 - role={}, project={}, override={}",
                    role, projectPermission, override);
            throw new ForbiddenException(StepErrorCode.STEP_ACCESS_DENIED);
        }
        return permission;
    }

    /** 편집용. EDITOR 가 아니면 STEP_EDIT_DENIED 로 거부한다. */
    public MemberPermission requireEditable(String role, MemberPermission projectPermission,
                                            MemberPermission override) {
        MemberPermission permission = requireAccess(role, projectPermission, override);
        if (permission != MemberPermission.EDITOR) {
            log.warn("스텝 편집 권한 없음 - role={}, project={}, override={}",
                    role, projectPermission, override);
            throw new ForbiddenException(StepErrorCode.STEP_EDIT_DENIED);
        }
        return permission;
    }
}