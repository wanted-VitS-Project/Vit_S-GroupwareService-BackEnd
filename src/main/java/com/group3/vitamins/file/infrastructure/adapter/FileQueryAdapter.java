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
}
