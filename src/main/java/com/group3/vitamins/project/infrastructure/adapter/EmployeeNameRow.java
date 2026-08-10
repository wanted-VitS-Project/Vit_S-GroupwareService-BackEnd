package com.group3.vitamins.project.infrastructure.adapter;

/** @param deleted 사원이 논리 삭제됐는지. 퇴사({@code resigned_at})와 다른 값이다. */
public record EmployeeNameRow(String userId, String name, boolean deleted) {
}
