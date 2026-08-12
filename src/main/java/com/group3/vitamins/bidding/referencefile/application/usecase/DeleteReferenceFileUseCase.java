package com.group3.vitamins.bidding.referencefile.application.usecase;

import com.group3.vitamins.bidding.referencefile.application.command.DeleteReferenceFileCommand;

public interface DeleteReferenceFileUseCase {

    void delete(DeleteReferenceFileCommand command);
}