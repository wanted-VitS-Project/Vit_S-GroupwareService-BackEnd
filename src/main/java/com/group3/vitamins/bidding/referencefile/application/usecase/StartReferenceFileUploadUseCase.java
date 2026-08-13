package com.group3.vitamins.bidding.referencefile.application.usecase;

import com.group3.vitamins.bidding.referencefile.application.command.StartReferenceFileUploadCommand;
import com.group3.vitamins.bidding.referencefile.application.result.StartReferenceFileUploadResult;

public interface StartReferenceFileUploadUseCase {

    StartReferenceFileUploadResult start(StartReferenceFileUploadCommand command);
}