package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.project.step.application.usecase.StepPermissionCleanupUseCase;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StepPermissionCleanupService implements StepPermissionCleanupUseCase {

    private final StepPermissionRepository stepPermissionRepository;

    @Override
    public void deleteByProjectIdAndUserId(Long projectId, String userId) {
        stepPermissionRepository.deleteByProjectIdAndUserId(projectId, userId);
    }
}
