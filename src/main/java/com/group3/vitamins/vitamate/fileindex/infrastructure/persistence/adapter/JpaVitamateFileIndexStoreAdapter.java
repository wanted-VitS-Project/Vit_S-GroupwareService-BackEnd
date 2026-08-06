package com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.entity.FileIndexEntity;
import com.group3.vitamins.vitamate.fileindex.infrastructure.persistence.repository.FileIndexJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// file_index 테이블에 인덱싱 상태를 저장하는 Adapter
@Component
@RequiredArgsConstructor
public class JpaVitamateFileIndexStoreAdapter implements VitamateFileIndexStorePort {

    private final FileIndexJpaRepository fileIndexJpaRepository;

    @Override
    public boolean existsFileVersion(Long fileVersionId) {
        // native query의 boolean 변환 흔들림을 피하기 위해 count 기준으로 존재 여부를 판단한다.
        return fileIndexJpaRepository.countActiveFileVersion(fileVersionId) > 0;
    }

    @Override
    public FileIndexStatus upsertStatus(
            Long fileVersionId,
            FileIndexStatus indexStatus,
            String errorMessage,
            LocalDateTime now
    ) {
        FileIndexEntity entity = fileIndexJpaRepository.findById(fileVersionId)
                .orElseGet(() -> new FileIndexEntity(fileVersionId, now));

        entity.changeStatus(indexStatus, errorMessage, now);
        return fileIndexJpaRepository.save(entity).getIndexStatus();
    }
}