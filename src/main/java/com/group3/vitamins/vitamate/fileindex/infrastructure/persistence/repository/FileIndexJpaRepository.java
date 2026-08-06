package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.entity.FileIndexEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// file_index 저장과 file_version 존재 확인을 담당하는 JPA Repository
public interface FileIndexJpaRepository extends JpaRepository<FileIndexEntity, Long> {

    @Query(value = """
        SELECT COUNT(*)
          FROM file_version
         WHERE file_version_id = :fileVersionId
           AND deleted_at IS NULL
        """, nativeQuery = true)
    long countActiveFileVersion(@Param("fileVersionId") Long fileVersionId);
}