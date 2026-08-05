package com.group3.vitamins.project.block.domain.model;

public enum BlockType {

    TEXT,
    IMAGE,
    FILE,
    CHECKLIST,
    PAYMENT_CONFIRM,
    TAX_INVOICE_VIEW,
    PERFORMANCE_VIEW,
    APPROVAL,
    AI,
    BID_NOTICE;

    /** 사용자가 직접 만들 수 있는 타입인지. BID_NOTICE 는 공고→프로젝트 전환 API 만 생성한다. */
    public boolean userCreatable() {
        return this != BID_NOTICE;
    }

    /** 스텝당 1개만 허용되는 타입인지 (PCB-001B · TXL-001B). */
    public boolean singlePerStep() {
        return this == PAYMENT_CONFIRM || this == TAX_INVOICE_VIEW;
    }
}