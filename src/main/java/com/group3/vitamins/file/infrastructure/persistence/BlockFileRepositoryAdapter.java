package com.group3.vitamins.file.infrastructure.persistence;

import com.group3.vitamins.file.domain.repository.BlockFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BlockFileRepositoryAdapter implements BlockFileRepository {

    private final SpringDataBlockFileRepository springDataRepository;

    @Override
    public void link(Long blockId, Long fileId, String linkedBy) {
        springDataRepository.save(new BlockFileJpaEntity(blockId, fileId, linkedBy));
    }
}
