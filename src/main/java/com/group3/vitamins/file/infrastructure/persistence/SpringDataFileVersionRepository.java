package com.group3.vitamins.file.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataFileVersionRepository extends JpaRepository<FileVersionJpaEntity, Long> {

    /** 해당 문서의 현재 최대 버전 차수. 버전이 없으면 null (어댑터가 0 으로 눕힌다). */
    @Query("SELECT MAX(v.versionNo) FROM FileVersionJpaEntity v WHERE v.fileId = :fileId")
    Integer findMaxVersionNo(@Param("fileId") Long fileId);

    /** 문서의 모든 버전(상태 무관) — §7 저장소 키 수집용. */
    List<FileVersionJpaEntity> findByFileId(Long fileId);

    /** 문서의 모든 버전 행을 벌크 물리 삭제한다(§7). 즉시 실행돼 file 삭제 전에 FK 참조를 끊는다. */
    @Modifying
    @Query("DELETE FROM FileVersionJpaEntity v WHERE v.fileId = :fileId")
    void deleteByFileId(@Param("fileId") Long fileId);
}
