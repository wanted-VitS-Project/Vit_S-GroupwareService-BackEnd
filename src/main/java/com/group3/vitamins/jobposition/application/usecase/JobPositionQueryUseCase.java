package com.group3.vitamins.jobposition.application.usecase;

import com.group3.vitamins.jobposition.application.query.JobPositionListQuery;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;

import java.util.List;

public interface JobPositionQueryUseCase {

    List<JobPositionResult> listJobPositions(JobPositionListQuery query);
}
