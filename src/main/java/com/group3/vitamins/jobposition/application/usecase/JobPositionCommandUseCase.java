package com.group3.vitamins.jobposition.application.usecase;

import com.group3.vitamins.jobposition.application.command.CreateJobPositionCommand;
import com.group3.vitamins.jobposition.application.command.DeleteJobPositionCommand;
import com.group3.vitamins.jobposition.application.command.UpdateJobPositionCommand;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;

public interface JobPositionCommandUseCase {

    JobPositionResult createJobPosition(CreateJobPositionCommand command);

    JobPositionResult updateJobPosition(UpdateJobPositionCommand command);

    void deleteJobPosition(DeleteJobPositionCommand command);
}
