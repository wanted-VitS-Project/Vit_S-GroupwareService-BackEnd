package com.group3.vitamins.employee.domain.model;

/**
 * 학위 (`employee.md` §2·§3 · HR-V1 QUAL). 마스터가 아니라 고정 enum 이다.
 *
 * <p>API 계약은 영문 코드({@code BACHELOR}·{@code MASTER}·{@code DOCTOR})로 주고받는다.
 * 엑셀 일괄 등록(블록3)의 한글 표기(학사·석사·박사) 변환은 파서가 담당한다.
 */
public enum Degree {
    BACHELOR("학사"),
    MASTER("석사"),
    DOCTOR("박사");

    private final String koreanLabel;

    Degree(String koreanLabel) {
        this.koreanLabel = koreanLabel;
    }

    public String koreanLabel() {
        return koreanLabel;
    }

    /** 엑셀 일괄 등록의 한글 학위 표기(학사·석사·박사) → enum. 미지원 표기는 {@code null}(호출자가 형식 오류 처리). */
    public static Degree fromKoreanLabel(String label) {
        if (label == null) {
            return null;
        }
        for (Degree degree : values()) {
            if (degree.koreanLabel.equals(label)) {
                return degree;
            }
        }
        return null;
    }
}
