package com.group3.vitamins.approval.application.result;

import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;

import java.util.List;

/**
 * SUB-005~008 재상신 회차 생성 결과.
 *
 * @param created {@code true} 면 새로 만든 것(201), {@code false} 면 이미 있던 DRAFT 회차를 그대로 반환한 것(200, SUB-008 멱등)
 */
public record ApprovalResubmissionResult(
        ApprovalRevision revision,
        List<ApprovalDocument> documents,
        List<ApprovalLineView> lines,
        int copiedFromRevisionNo,
        boolean created
) {
}
