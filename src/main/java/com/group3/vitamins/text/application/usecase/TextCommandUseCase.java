package com.group3.vitamins.text.application.usecase;

import com.group3.vitamins.text.application.command.DeleteTextBlockCommand;
import com.group3.vitamins.text.application.command.UpdateTextContentCommand;
import com.group3.vitamins.text.domain.model.Text;

public interface TextCommandUseCase {

    Text updateContent(UpdateTextContentCommand command);
}
