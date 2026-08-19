package com.group3.vitamins.qualification.domain.model;

/**
 * 전공·자격증 마스터 <b>이름 규칙</b> (qualification.md 공통 원칙 · HR-V1 MAJ-001·CRT-001).
 *
 * <p>major·certificate 두 마스터와, 이름만으로 마스터를 만드는 사원 엑셀 자동 생성 경로(employee.md §7·§8)가 공유한다.
 * 순수 규칙 — Spring·JPA 비의존.
 *
 * <p>금지 문자는 사원 엑셀이 셀 안에서 <b>항목 구분자</b>(`,` `;` 줄바꿈)와 <b>전공:학위 구분자</b>(`:`)로 쓰는 문자다.
 * 이름에 들어가면 엑셀 파서가 그 자리에서 쪼개므로 마스터에 존재해도 매칭이 안 된다.
 */
public final class QualificationNameRule {

    public static final int MAX_LENGTH = 100;

    /** 이름에 쓸 수 없는 문자 — 엑셀 항목 구분자(`,` `;` `\r` `\n`)와 `전공:학위` 구분자(`:`). */
    private static final char[] FORBIDDEN_CHARS = {',', ';', ':', '\r', '\n'};

    private QualificationNameRule() {
    }

    /** 앞뒤 공백을 걷어낸 저장용 이름. 검사는 이 값을 기준으로 한다. */
    public static String normalize(String name) {
        return name == null ? null : name.strip();
    }

    /** 저장 가능한 이름인지 — 비어 있지 않고, {@link #MAX_LENGTH} 이하이며, 금지 문자가 없다. */
    public static boolean isValid(String name) {
        String n = normalize(name);
        if (n == null || n.isEmpty() || n.length() > MAX_LENGTH) {
            return false;
        }
        for (char c : FORBIDDEN_CHARS) {
            if (n.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }
}
