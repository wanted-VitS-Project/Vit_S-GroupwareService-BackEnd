package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.file.application.port.UploaderLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UploaderLookupAdapter implements UploaderLookupPort {

    private final UploaderLookupMapper uploaderLookupMapper;

    @Override
    public Optional<UploaderSnapshot> findByUserId(String userId) {
        UploaderRow row = uploaderLookupMapper.findByUserId(userId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new UploaderSnapshot(row.name(), row.department(), row.position()));
    }
}
