package com.group3.vitamins.file.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataFileVersionRepository extends JpaRepository<FileVersionJpaEntity, Long> {

    /** 해당 문서의 현재 최대 버전 차수. 버전이 없으면 null (어댑터가 0 으로 눕힌다). */
    @Query("SELECT MAX(v.versionNo) FROM FileVersionJpaEntity v WHERE v.fileId = :fileId")
    Integer findMaxVersionNo(@Param("fileId") Long fileId);
}
