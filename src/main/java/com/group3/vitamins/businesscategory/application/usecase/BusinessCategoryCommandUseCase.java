package com.group3.vitamins.businesscategory.application.usecase;

import com.group3.vitamins.businesscategory.application.command.CreateBusinessCategoryCommand;
import com.group3.vitamins.businesscategory.application.command.UpdateBusinessCategoryCommand;
import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;

public interface BusinessCategoryCommandUseCase {

    BusinessCategoryResult createCategory(CreateBusinessCategoryCommand command);

    BusinessCategoryResult updateCategory(UpdateBusinessCategoryCommand command);
}