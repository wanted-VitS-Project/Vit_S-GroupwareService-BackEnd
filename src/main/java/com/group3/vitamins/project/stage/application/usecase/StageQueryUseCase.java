package com.group3.vitamins.project.stage.application.usecase;

import com.group3.vitamins.project.stage.application.query.StageListQuery;
import com.group3.vitamins.project.stage.application.result.StageSummary;

import java.util.List;

public interface StageQueryUseCase {

    List<StageSummary> getStages(StageListQuery query);
}