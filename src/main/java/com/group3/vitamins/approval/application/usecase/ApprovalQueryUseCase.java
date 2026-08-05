package com.group3.vitamins.approval.application.usecase;

import com.group3.vitamins.approval.application.query.GetApprovalRevisionQuery;
import com.group3.vitamins.approval.application.result.ApprovalRevisionDetail;

public interface ApprovalQueryUseCase {

    ApprovalRevisionDetail getRevisionDetail(GetApprovalRevisionQuery query);
}
