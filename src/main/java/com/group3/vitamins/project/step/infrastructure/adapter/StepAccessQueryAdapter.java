package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.port.StepAccessQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StepAccessQueryAdapter implements StepAccessQueryPort {

    private final StepAccessQueryMapper stepAccessQueryMapper;

    @Override
    public Optional<StepAccessSnapshot> findAccess(Long stepId, String requesterUserId, Long companyId) {
        StepAccessRow row = stepAccessQueryMapper.findAccess(stepId, requesterUserId, companyId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new StepAccessSnapshot(
                row.stepId(), row.projectId(), row.projectVisible(),
                toPermission(row.memberPermission()),
                toPermission(row.overridePermission())));
    }

    /**
     * 행이 없으면 {@code null} 그대로 넘긴다 — 판정은 정책이 한다.
     *
     * <p>⚠️ {@code null} 을 {@code NONE} 으로 바꾸지 마라. {@code null} 은 "행이 없어 상위 권한을
     * 상속" 이고 {@code NONE} 은 "행이 있고 명시적으로 차단" 이다. 뭉개면 차단해둔 스텝이 열린다
     * ({@code StepAccessPolicy#resolve} 의 {@code override != null ? override : projectPermission}).
     */
    private MemberPermission toPermission(String permission) {
        return permission == null ? null : MemberPermission.valueOf(permission);
    }
}
