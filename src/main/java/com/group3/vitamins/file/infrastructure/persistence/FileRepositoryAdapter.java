package com.group3.vitamins.file.infrastructure.persistence;

import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FileRepositoryAdapter implements FileRepository {

    private final SpringDataFileRepository springDataRepository;

    @Override
    public File save(File file) {
        // saveAndFlush — 제약 위반(FK 등)을 커밋이 아니라 쓰기 시점에 발생시켜 서비스가 변환할 수 있게 한다.
        return FilePersistenceMapper.toDomain(
                springDataRepository.saveAndFlush(FilePersistenceMapper.toEntity(file)));
    }

    @Override
    public Optional<File> findById(Long fileId) {
        return springDataRepository.findById(fileId)
                .map(FilePersistenceMapper::toDomain);
    }

    @Override
    public int renameIfVersionMatches(Long fileId, String name, int expectedVersion) {
        return springDataRepository.renameIfVersionMatches(fileId, name, expectedVersion);
    }

    @Override
    public Optional<Integer> lockCurrentVersion(Long fileId) {
        return springDataRepository.findForUpdate(fileId).map(FileJpaEntity::getVersion);
    }

    @Override
    public void deleteById(Long fileId) {
        // deleteById + flush — file DELETE(및 block_file CASCADE)를 쓰기 시점에 즉시 실행한다.
        // 저장소(S3) 삭제 전에 DB 제약을 확정시켜, FK 문제가 있으면 여기서 터지게 한다(지연 flush 함정 회피).
        springDataRepository.deleteById(fileId);
        springDataRepository.flush();
    }
}
