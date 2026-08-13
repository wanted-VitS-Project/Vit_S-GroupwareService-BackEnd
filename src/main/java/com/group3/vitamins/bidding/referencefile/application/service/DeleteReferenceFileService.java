package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.referencefile.application.command.DeleteReferenceFileCommand;
import com.group3.vitamins.bidding.referencefile.application.port.BidReferenceFileActiveUsagePort;
import com.group3.vitamins.bidding.referencefile.application.usecase.DeleteReferenceFileUseCase;
import com.group3.vitamins.bidding.referencefile.domain.exception.BidReferenceFileErrorCode;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class DeleteReferenceFileService implements DeleteReferenceFileUseCase {

    private final BidReferenceFileRepository referenceFileRepository;
    private final BidReferenceFileActiveUsagePort activeUsagePort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    @Override
    public void delete(DeleteReferenceFileCommand command) {
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        // 삭제를 위한 배타적 조회 — 이 트랜잭션이 끝날 때까지 검토 생성이 같은 파일을 선택하지 못하게 막는다.
        BidReferenceFile referenceFile = referenceFileRepository
                .findActiveByIdAndCompanyIdForDeletion(command.referenceFileId(), companyId)
                .orElseThrow(() -> new NotFoundException(
                        BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_NOT_FOUND
                ));

        if (activeUsagePort.existsActiveReviewUsage(companyId, command.referenceFileId())) {
            throw new ConflictException(
                    BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_IN_USE
            );
        }

        referenceFileRepository.saveDeletedWithCleanupOutbox(
                referenceFile.delete(LocalDateTime.now(clock))
        );
    }
}