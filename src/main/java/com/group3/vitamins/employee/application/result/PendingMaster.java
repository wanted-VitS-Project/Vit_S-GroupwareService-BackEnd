package com.group3.vitamins.employee.application.result;

/**
 * 엑셀 일괄 등록에서 <b>자동 생성 대상(또는 생성된) 마스터 한 건</b> (employee.md §7 {@code newMasters} · §8 {@code createdMasters}).
 *
 * @param name     전공명 또는 자격증명 (셀 값 trim)
 * @param rowCount 그 이름을 참조하는 <b>유효 행</b> 수 — 관리자가 등록 전에 "이 이름이 몇 명에게 붙는지" 보고 오타를 잡는 용도
 */
public record PendingMaster(String name, int rowCount) {
}
