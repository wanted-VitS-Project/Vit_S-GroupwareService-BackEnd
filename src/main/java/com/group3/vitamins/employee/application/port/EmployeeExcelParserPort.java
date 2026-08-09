package com.group3.vitamins.employee.application.port;

import com.group3.vitamins.employee.application.result.ParsedEmployeeRow;

import java.util.List;

/**
 * 업로드된 엑셀(.xlsx·.xls)을 <b>원시 행 목록으로 파싱</b>하는 아웃바운드 포트 (employee.md §7·§8).
 * 헤더 아래 데이터 행만 읽고, 완전히 빈 행은 건너뛴다. 열기 실패(형식 손상 등)는 {@code EMP_FILE_TYPE_INVALID} 로 던진다.
 * POI 어댑터가 구현한다(엑셀 라이브러리를 서비스에 노출하지 않는다).
 */
public interface EmployeeExcelParserPort {

    List<ParsedEmployeeRow> parse(byte[] content);
}
