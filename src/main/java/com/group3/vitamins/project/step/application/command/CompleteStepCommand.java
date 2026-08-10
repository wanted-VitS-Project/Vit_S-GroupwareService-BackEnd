package com.group3.vitamins.project.step.application.command;

/** 스텝 완료 처리. openIssueAction 누락은 요청 DTO 가, 오타는 서비스가 잡는다(에러코드가 다르다). */
public record CompleteStepCommand(
        Long stepId,
        String openIssueAction,
        String requesterUserId,
        String role
) {
}
