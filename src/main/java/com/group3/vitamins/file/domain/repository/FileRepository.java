package com.group3.vitamins.file.domain.repository;

import com.group3.vitamins.file.domain.model.File;

import java.util.Optional;

/**
 * 파일(논리 문서) 영속성 아웃바운드 포트.
 *
 * <p>구현체는 {@code infrastructure/persistence/FileRepositoryAdapter} (JPA · saveAndFlush).
 * 블록 내 동명 검사·목록 같은 화면용 조회는 별도 MyBatis 조회 포트가 담당한다(§3 구현 시점).
 */
public interface FileRepository {

    /** 생성·수정을 저장한다. 제약 위반을 쓰기 시점에 동기 발생시켜야 하므로 어댑터는 saveAndFlush 로 구현한다. */
    File save(File file);

    /** 삭제 여부와 무관하게 문서를 찾는다(복구·상태 판정은 서비스가 deletedAt 으로 한다). */
    Optional<File> findById(Long fileId);
}
