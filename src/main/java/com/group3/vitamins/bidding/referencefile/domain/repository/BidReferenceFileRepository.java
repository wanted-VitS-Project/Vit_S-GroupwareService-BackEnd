package com.group3.vitamins.bidding.referencefile.domain.repository;

import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;

import java.util.List;
import java.util.Optional;

public interface BidReferenceFileRepository {

    BidReferenceFile save(BidReferenceFile referenceFile);

    Optional<BidReferenceFile> findByIdAndCompanyId(Long referenceFileId, Long companyId);

    // 검토 생성과의 삭제 경합을 막기 위해 행을 잠그고 조회합니다.
    Optional<BidReferenceFile> findByIdAndCompanyIdForUpdate(Long referenceFileId, Long companyId);

    List<BidReferenceFile> findAllActiveByCompanyId(Long companyId);

    // 업로드 완료 상태와 인덱싱 요청 Outbox를 한 트랜잭션으로 저장합니다.
    BidReferenceFile saveCompletedWithIndexOutbox(BidReferenceFile referenceFile);

    // 논리 삭제 상태와 정리 요청 Outbox를 한 트랜잭션으로 저장합니다.
    BidReferenceFile saveDeletedWithCleanupOutbox(BidReferenceFile referenceFile);
}