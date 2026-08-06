package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.result.BlockFileProjection;
import com.group3.vitamins.file.application.result.FileVersionProjection;
import com.group3.vitamins.file.application.result.ProjectFileVersionProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FileQueryAdapter implements FileQueryPort {

    private final FileQueryMapper fileQueryMapper;

    @Override
    public boolean existsActiveNameInBlock(Long blockId, String name) {
        return fileQueryMapper.existsActiveNameInBlock(blockId, name);
    }

    @Override
    public Optional<Long> findBlockIdByFileId(Long fileId) {
        return Optional.ofNullable(fileQueryMapper.findBlockIdByFileId(fileId));
    }

    @Override
    public List<FileVersionProjection> findCompletedVersions(Long fileId) {
        return fileQueryMapper.findCompletedVersions(fileId);
    }

    @Override
    public int findMaxCompletedVersionNo(Long fileId) {
        return fileQueryMapper.findMaxCompletedVersionNo(fileId);
    }

    @Override
    public List<BlockFileProjection> findBlockFiles(Long blockId, boolean deleted) {
        return fileQueryMapper.findBlockFiles(blockId, deleted);
    }

    @Override
    public List<ProjectFileVersionProjection> findProjectFileVersions(Long projectId) {
        return fileQueryMapper.findProjectFileVersions(projectId);
    }
}
