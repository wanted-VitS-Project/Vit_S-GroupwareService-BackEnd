package com.group3.vitamins.file.infrastructure.persistence;

import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileVersionRepositoryAdapter implements FileVersionRepository {

    private final SpringDataFileVersionRepository springDataRepository;

    @Override
    public FileVersion save(FileVersion fileVersion) {
        return FileVersionPersistenceMapper.toDomain(
                springDataRepository.saveAndFlush(FileVersionPersistenceMapper.toEntity(fileVersion)));
    }

    @Override
    public Optional<FileVersion> findById(Long fileVersionId) {
        return springDataRepository.findById(fileVersionId)
                .map(FileVersionPersistenceMapper::toDomain);
    }

    @Override
    public int findMaxVersionNo(Long fileId) {
        Integer max = springDataRepository.findMaxVersionNo(fileId);
        return max == null ? 0 : max;
    }
}
