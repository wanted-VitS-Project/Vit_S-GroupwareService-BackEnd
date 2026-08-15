package com.group3.vitamins.bidding.collectioncondition.domain.model;

// 자동·수동 수집이 실행마다 얼마나 되돌아가 검색할지 정의합니다.
public enum CollectionLookbackPeriod {
    ONE_WEEK(7),
    TWO_WEEKS(14),
    ONE_MONTH(30);

    private final int days;

    CollectionLookbackPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
