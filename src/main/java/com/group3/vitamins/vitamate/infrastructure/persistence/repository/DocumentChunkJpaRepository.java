package com.group3.vitamins.vitamate.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.infrastructure.persistence.entity.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

// 문서 청크가 선택 문서 버전에 속하는지 검증하는 JPA Repository
public interface DocumentChunkJpaRepository extends JpaRepository<DocumentChunkEntity, Long> {

    // 청크 ID와 파일 버전 ID가 서로 연결되어 있고 삭제되지 않았는지 확인한다.
    boolean existsByIdAndFileVersionIdAndDeletedAtIsNull(Long id, Long fileVersionId);
}
