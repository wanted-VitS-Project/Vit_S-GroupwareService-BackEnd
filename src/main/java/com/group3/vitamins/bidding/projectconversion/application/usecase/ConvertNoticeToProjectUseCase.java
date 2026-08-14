package com.group3.vitamins.bidding.projectconversion.application.usecase;

import com.group3.vitamins.bidding.projectconversion.application.command.ConvertNoticeToProjectCommand;
import com.group3.vitamins.bidding.projectconversion.application.result.ConvertNoticeToProjectResult;

public interface ConvertNoticeToProjectUseCase {

    ConvertNoticeToProjectResult convert(ConvertNoticeToProjectCommand command);
}
