package com.group3.vitamins.project.step.infrastructure.adapter;

import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import com.group3.vitamins.project.step.application.port.StageLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 스테이지 애그리게이트 조회를 스텝 모듈에 중계한다.
 * 논리 삭제 조건을 복제하지 않기 위해 스테이지 쪽 리포지토리에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class StageLookupAdapter implements StageLookupPort {

    private final StageRepository stageRepository;

    @Override
    public boolean existsInProject(Long stageId, Long projectId) {
        return stageRepository.existsInProject(stageId, projectId);
    }
}