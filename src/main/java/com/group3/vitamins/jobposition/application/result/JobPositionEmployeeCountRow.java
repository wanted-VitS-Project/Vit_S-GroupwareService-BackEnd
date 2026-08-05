package com.group3.vitamins.jobposition.application.result;

/**
 * 직급별 사용 인원 집계 한 행 (MyBatis 결과). record 는 setter 가 없어 XML 에서 생성자 인자로 매핑한다.
 */
public record JobPositionEmployeeCountRow(
        Long jobPositionId,
        int employeeCount
) {
}
