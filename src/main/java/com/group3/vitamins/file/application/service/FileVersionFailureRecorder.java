package com.group3.vitamins.file.application.service;

import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 업로드 완료 통보 실패(§2)를 <b>호출 트랜잭션과 분리해</b> 기록한다.
 *
 * <p>완료 통보 서비스는 {@code @Transactional} 이라, 실패를 알리며 예외를 던지면 같은 트랜잭션이
 * 롤백돼 {@code FAILED} 전이가 사라진다(버전이 {@code UPLOADING} 으로 남아 12h 정리 대상이 됨).
 * {@link Propagation#REQUIRES_NEW} 로 별도 트랜잭션에서 커밋해 실패 상태를 확정 저장한다.
 * self-invocation 으로는 propagation 이 안 걸리므로 <b>별도 빈</b>으로 둔다.
 */
@Component
@RequiredArgsConstructor
public class FileVersionFailureRecorder {

    private final FileVersionRepository fileVersionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(FileVersion version) {
        version.fail();
        fileVersionRepository.save(version);
    }
}
