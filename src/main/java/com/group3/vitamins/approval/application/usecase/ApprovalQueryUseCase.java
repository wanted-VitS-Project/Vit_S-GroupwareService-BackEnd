package com.group3.vitamins.approval.application.usecase;

import com.group3.vitamins.approval.application.query.GetApprovalDetailQuery;
import com.group3.vitamins.approval.application.query.GetApprovalHistoryQuery;
import com.group3.vitamins.approval.application.query.GetApprovalRevisionQuery;
import com.group3.vitamins.approval.application.query.ListApprovalsQuery;
import com.group3.vitamins.approval.application.result.ApprovalDetailResult;
import com.group3.vitamins.approval.application.result.ApprovalHistoryResult;
import com.group3.vitamins.approval.application.result.ApprovalListPageResult;
import com.group3.vitamins.approval.application.result.ApprovalRevisionDetail;

public interface ApprovalQueryUseCase {

    ApprovalRevisionDetail getRevisionDetail(GetApprovalRevisionQuery query);

    ApprovalListPageResult listApprovals(ListApprovalsQuery query);

    ApprovalDetailResult getApprovalDetail(GetApprovalDetailQuery query);

    ApprovalHistoryResult getApprovalHistory(GetApprovalHistoryQuery query);
}
