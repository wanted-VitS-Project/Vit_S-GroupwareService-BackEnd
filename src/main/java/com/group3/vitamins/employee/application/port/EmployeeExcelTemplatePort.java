package com.group3.vitamins.employee.application.port;

/**
 * 사원 일괄 등록 <b>엑셀 템플릿(.xlsx)을 생성</b>하는 아웃바운드 포트 (employee.md §6).
 * 헤더만 있는 빈 템플릿의 바이너리를 돌려준다. POI 어댑터가 구현한다(엑셀 라이브러리를 도메인/서비스에 노출하지 않는다).
 */
public interface EmployeeExcelTemplatePort {

    /** 헤더 행만 채운 .xlsx 바이너리. */
    byte[] generate();
}
