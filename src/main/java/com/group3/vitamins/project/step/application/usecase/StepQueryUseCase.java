package com.group3.vitamins.project.step.application.usecase;

import com.group3.vitamins.project.step.application.query.StepDetailQuery;
import com.group3.vitamins.project.step.application.query.StepListQuery;
import com.group3.vitamins.project.step.application.result.StepDetailResult;
import com.group3.vitamins.project.step.application.result.StepSummary;

import java.util.List;

public interface StepQueryUseCase {

    List<StepSummary> getSteps(StepListQuery query);

    StepDetailResult getStepDetail(StepDetailQuery query);
}