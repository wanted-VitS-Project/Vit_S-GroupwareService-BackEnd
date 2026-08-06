package com.group3.vitamins.file.infrastructure.adapter;

/** 업로더 스냅샷 조회 행 (employee ⋈ department ⋈ job_position). 부서·직책은 미배정이면 null. */
public record UploaderRow(String name, String department, String position) {
}
