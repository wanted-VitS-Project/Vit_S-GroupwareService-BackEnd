package com.group3.vitamins.approval.application.usecase;

import com.group3.vitamins.approval.application.command.AddApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.ApproveApprovalLineCommand;
import com.group3.vitamins.approval.application.command.RejectApprovalLineCommand;
import com.group3.vitamins.approval.application.command.RemoveApprovalDocumentCommand;
import com.group3.vitamins.approval.application.command.ResubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.SubmitApprovalCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalLinesCommand;
import com.group3.vitamins.approval.application.command.UpdateApprovalRevisionCommand;
import com.group3.vitamins.approval.application.result.ApprovalDocumentView;
import com.group3.vitamins.approval.application.result.ApprovalLineProcessResult;
import com.group3.vitamins.approval.application.result.ApprovalLineView;
import com.group3.vitamins.approval.application.result.ApprovalResubmissionResult;
import com.group3.vitamins.approval.application.result.ApprovalSubmissionResult;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;

import java.util.List;

/**
 * 결재 상세 생성은 이 유스케이스에 없다 — {@code block} 생성과 같은 트랜잭션에서
 * {@code BlockDetailPort.createDetail()}(→ {@code ApprovalBlockDetailAdapter})을 통해 이뤄진다.
 */
public interface ApprovalCommandUseCase {

    ApprovalRevision updateRevisionDraft(UpdateApprovalRevisionCommand command);

    List<ApprovalLineView> updateLines(UpdateApprovalLinesCommand command);

    ApprovalResubmissionResult resubmit(ResubmitApprovalCommand command);

    ApprovalDocumentView addDocument(AddApprovalDocumentCommand command);

    void removeDocument(RemoveApprovalDocumentCommand command);

    ApprovalSubmissionResult submit(SubmitApprovalCommand command);

    ApprovalLineProcessResult approve(ApproveApprovalLineCommand command);

    ApprovalLineProcessResult reject(RejectApprovalLineCommand command);
}
