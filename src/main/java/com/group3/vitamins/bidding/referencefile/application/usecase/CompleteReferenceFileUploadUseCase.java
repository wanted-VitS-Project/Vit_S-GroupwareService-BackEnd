package com.group3.vitamins.bidding.referencefile.application.usecase;

import com.group3.vitamins.bidding.referencefile.application.command.CompleteReferenceFileUploadCommand;
import com.group3.vitamins.bidding.referencefile.application.result.CompleteReferenceFileUploadResult;

public interface CompleteReferenceFileUploadUseCase {

    CompleteReferenceFileUploadResult complete(CompleteReferenceFileUploadCommand command);
}