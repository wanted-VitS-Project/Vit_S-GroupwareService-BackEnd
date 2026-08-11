package com.group3.vitamins.file.application.port;

import java.util.Optional;

/**
 * 업로더 스냅샷 조회 아웃바운드 포트. 버전에 이름·부서·직책을 박아 이후 소속 변경·퇴사에도 이력이 안 바뀐다(VER-006).
 * 구현은 {@code infrastructure/adapter/UploaderLookupAdapter} (MyBatis · employee+department+job_position 조인).
 */
public interface UploaderLookupPort {

    Optional<UploaderSnapshot> findByUserId(String userId);

    /** 업로더 스냅샷. name 은 NOT NULL, 부서·직책은 미배정일 수 있어 nullable. */
    record UploaderSnapshot(String name, String department, String position) {
    }
}
