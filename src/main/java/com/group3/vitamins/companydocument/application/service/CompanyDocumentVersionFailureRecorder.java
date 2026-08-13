package com.group3.vitamins.companydocument.application.service;

import com.group3.vitamins.companydocument.domain.model.CompanyDocumentVersion;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사내 문서 업로드 완료 통보 실패(§2)를 <b>호출 트랜잭션과 분리해</b> 기록한다.
 *
 * <p>완료 통보 서비스는 {@code @Transactional} 이라, 실패를 알리며 예외를 던지면 같은 트랜잭션이 롤백돼
 * {@code FAILED} 전이가 사라진다. {@link Propagation#REQUIRES_NEW} 로 별도 트랜잭션에서 커밋해 실패 상태를
 * 확정 저장한다. self-invocation 으로는 propagation 이 안 걸리므로 <b>별도 빈</b>으로 둔다(file 선례와 동일).
 */
@Component
@RequiredArgsConstructor
public class CompanyDocumentVersionFailureRecorder {

    private final CompanyDocumentVersionRepository versionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(CompanyDocumentVersion version) {
        version.fail();
        versionRepository.save(version);
    }
}
