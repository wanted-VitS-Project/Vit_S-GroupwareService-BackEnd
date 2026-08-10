package com.group3.vitamins.project.step.domain.model;

/** 스텝 완료 시 미완료 이슈를 어떻게 할지 (STP-006). 이슈가 남아도 완료 자체는 막지 않는다(STP-005). */
public enum OpenIssueAction {
    KEEP, CLOSE
}