package com.group3.vitamins.approval.application.usecase;

import com.group3.vitamins.approval.application.command.AddApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.CreateApprovalCommand;
import com.group3.vitamins.approval.application.command.RemoveApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.ResubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.SubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalLinesCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalRevisionCommand;
import com.group3.vitamins.approval.application.result.ApprovalDocumentView;
import com.group3.vitamins.approval.application.result.ApprovalLineView;
import com.group3.vitamins.approval.application.result.ApprovalResubmissionResult;
import com.group3.vitamins.approval.application.result.ApprovalSubmissionResult;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalWithRevision;

import java.util.List;

public interface ApprovalCommandUseCase {

    ApprovalWithRevision createApproval(CreateApprovalCommand command);

    ApprovalRevision updateRevisionDraft(UpdateApprovalRevisionCommand command);

    List<ApprovalLineView> updateLines(UpdateApprovalLinesCommand command);

    ApprovalResubmissionResult resubmit(ResubmitApprovalCommand command);

    ApprovalDocumentView addDocument(AddApprovalDocumentCommand command);

    void removeDocument(RemoveApprovalDocumentCommand command);

    ApprovalSubmissionResult submit(SubmitApprovalCommand command);
}
