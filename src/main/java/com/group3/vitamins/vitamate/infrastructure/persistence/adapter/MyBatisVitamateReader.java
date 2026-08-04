package com.group3.vitamins.vitamate.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.application.port.VitamateBlockReader;
import com.group3.vitamins.vitamate.application.port.VitamateFileReader;
import com.group3.vitamins.vitamate.infrastructure.persistence.mapper.VitamateAnalysisMapper;
import com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateBlockContextRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

// 비타메이트 블록 권한과 파일 버전 검증 조회를 담당하는 MyBatis 어댑터
@Component
@RequiredArgsConstructor
public class MyBatisVitamateReader implements VitamateBlockReader, VitamateFileReader {

    private final VitamateAnalysisMapper mapper;

    @Override
    public Optional<VitamateBlockContext> findAccessibleVitamateBlock(Long blockId, String userId) {
        return Optional.ofNullable(mapper.findAccessibleVitamateBlock(blockId, userId))
                .map(this::toContext);
    }

    @Override
    public boolean existsAllCompletedFileVersionsInProject(Long projectId, List<Long> fileVersionIds) {
        if (fileVersionIds == null || fileVersionIds.isEmpty()) {
            return false;
        }

        int distinctRequestCount = new HashSet<>(fileVersionIds).size();
        int matchedCount = mapper.countCompletedFileVersionsInProject(projectId, fileVersionIds);
        return matchedCount == distinctRequestCount;
    }

    private VitamateBlockContext toContext(VitamateBlockContextRow row) {
        return new VitamateBlockContext(
                row.getBlockId(),
                row.getVitamateBlockId(),
                row.getStepId(),
                row.getProjectId()
        );
    }
}
