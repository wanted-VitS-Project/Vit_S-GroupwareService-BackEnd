package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.stage.application.usecase.StagePermissionDefaultUseCase;
import com.group3.vitamins.project.stage.domain.repository.StagePermissionDefaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class StagePermissionDefaultService implements StagePermissionDefaultUseCase {

    private final StagePermissionDefaultRepository stagePermissionDefaultRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, MemberPermission> findDefaults(Long stageId) {
        return stagePermissionDefaultRepository.findAllByStageId(stageId);
    }

    @Override
    public void deleteByProjectIdAndUserId(Long projectId, String userId) {
        stagePermissionDefaultRepository.deleteByProjectIdAndUserId(projectId, userId);
    }
}
