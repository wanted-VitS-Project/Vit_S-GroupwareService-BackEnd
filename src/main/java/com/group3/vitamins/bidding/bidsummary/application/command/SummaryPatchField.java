package com.group3.vitamins.bidding.bidsummary.application.command;

// 요약 PATCH 요청에서 필드 생략 여부와 전달값을 구분합니다.
public record SummaryPatchField(boolean present, String value) {

    public static SummaryPatchField absent() {
        return new SummaryPatchField(false, null);
    }

    public static SummaryPatchField of(String value) {
        return new SummaryPatchField(true, value);
    }

    public String resolve(String currentValue) {
        return present ? value : currentValue;
    }
}
