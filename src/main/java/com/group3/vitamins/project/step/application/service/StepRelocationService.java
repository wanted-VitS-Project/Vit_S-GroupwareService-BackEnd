package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.project.step.application.usecase.StepRelocationUseCase;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StepRelocationService implements StepRelocationUseCase {

    private final StepRepository stepRepository;

    @Override
    public int relocateByStage(Long fromStageId, Long toStageId) {
        List<Step> steps = stepRepository.findAllByStageId(fromStageId);

        LocalDateTime now = LocalDateTime.now();
        steps.forEach(step ->
                stepRepository.save(step.moveTo(toStageId, step.getSortOrder(), now)));

        return steps.size();
    }
}
