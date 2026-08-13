package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 업로드 완료 검증 실패를 호출 트랜잭션과 분리해 기록한다.
 *
 * <p>완료 서비스는 {@code @Transactional}이라 실패를 알리며 예외를 던지면 같은 트랜잭션이
 * 롤백돼 {@code FAILED} 전이가 사라진다({@code file.FileVersionFailureRecorder}와 동일한 문제).
 * {@link Propagation#REQUIRES_NEW}로 별도 트랜잭션에서 커밋한다. self-invocation으로는
 * propagation이 안 걸리므로 별도 빈으로 둔다.
 */
@Component
@RequiredArgsConstructor
public class BidReferenceFileFailureRecorder {

    private final BidReferenceFileRepository referenceFileRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markUploadFailed(BidReferenceFile referenceFile, LocalDateTime now) {
        referenceFileRepository.save(referenceFile.markUploadFailed(now));
    }
}
