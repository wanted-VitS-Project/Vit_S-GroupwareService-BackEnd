package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.port.FileQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileQueryAdapter implements FileQueryPort {

    private final FileQueryMapper fileQueryMapper;

    @Override
    public boolean existsActiveNameInBlock(Long blockId, String name) {
        return fileQueryMapper.existsActiveNameInBlock(blockId, name);
    }

    @Override
    public java.util.Optional<Long> findBlockIdByFileId(Long fileId) {
        return java.util.Optional.ofNullable(fileQueryMapper.findBlockIdByFileId(fileId));
    }

    @Override
    public java.util.List<com.group3.vitamins.file.application.result.FileVersionProjection> findCompletedVersions(Long fileId) {
        return fileQueryMapper.findCompletedVersions(fileId);
    }

    @Override
    public java.util.List<com.group3.vitamins.file.application.result.BlockFileProjection> findBlockFiles(Long blockId, boolean deleted) {
        return fileQueryMapper.findBlockFiles(blockId, deleted);
    }
}
