package com.group3.vitamins.bidding.referencefile.domain.repository;

import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;

import java.util.List;
import java.util.Optional;

public interface BidReferenceFileRepository {

    BidReferenceFile save(BidReferenceFile referenceFile);

    Optional<BidReferenceFile> findByIdAndCompanyId(Long referenceFileId, Long companyId);

    // 삭제를 위한 배타적 조회입니다. 검토 생성과 동시에 같은 파일을 처리하지 못하게 막습니다.
    // 잠금 방식(PESSIMISTIC_WRITE)은 구현 세부사항이라 인프라 어댑터에만 둔다.
    Optional<BidReferenceFile> findActiveByIdAndCompanyIdForDeletion(Long referenceFileId, Long companyId);

    List<BidReferenceFile> findAllActiveByCompanyId(Long companyId);

    // 업로드 완료 상태와 인덱싱 요청 Outbox를 한 트랜잭션으로 저장합니다.
    BidReferenceFile saveCompletedWithIndexOutbox(BidReferenceFile referenceFile);

    // 논리 삭제 상태와 정리 요청 Outbox를 한 트랜잭션으로 저장합니다.
    BidReferenceFile saveDeletedWithCleanupOutbox(BidReferenceFile referenceFile);
}