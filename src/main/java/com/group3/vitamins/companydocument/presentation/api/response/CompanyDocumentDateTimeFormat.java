package com.group3.vitamins.companydocument.presentation.api.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 사내 문서 응답 시각 포맷 공용 상수·변환 헬퍼.
 *
 * <p>목록·버전이력·버전상세·삭제 응답이 같은 {@code yyyy-MM-dd HH:mm:ss} 포맷을 쓴다. 각 record 가 개별 선언하면
 * 포맷을 바꿀 때 일부만 고쳐 표기가 갈라진다(CodeRabbit #357). 단일 출처로 모은다.
 */
public final class CompanyDocumentDateTimeFormat {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CompanyDocumentDateTimeFormat() {
    }

    /** null 이면 null, 아니면 포맷 문자열. */
    public static String format(LocalDateTime time) {
        return time == null ? null : time.format(FMT);
    }
}
