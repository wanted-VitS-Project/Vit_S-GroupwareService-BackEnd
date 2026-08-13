package com.group3.vitamins.bidding.referencefile.application.usecase;

import com.group3.vitamins.bidding.referencefile.application.query.GetReferenceFileListQuery;
import com.group3.vitamins.bidding.referencefile.application.result.ReferenceFileListResult;

public interface GetReferenceFileListUseCase {

    ReferenceFileListResult get(GetReferenceFileListQuery query);
}