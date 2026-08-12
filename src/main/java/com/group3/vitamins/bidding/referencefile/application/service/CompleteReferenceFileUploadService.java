package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.referencefile.application.command.CompleteReferenceFileUploadCommand;
import com.group3.vitamins.bidding.referencefile.application.result.CompleteReferenceFileUploadResult;
import com.group3.vitamins.bidding.referencefile.application.usecase.CompleteReferenceFileUploadUseCase;
import com.group3.vitamins.bidding.referencefile.domain.exception.BidReferenceFileErrorCode;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileUploadStatus;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
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
public class CompleteReferenceFileUploadService implements CompleteReferenceFileUploadUseCase {

    private final BidReferenceFileRepository referenceFileRepository;
    private final BidReferenceFileFailureRecorder failureRecorder;
    private final FileStoragePort fileStoragePort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    @Override
    public CompleteReferenceFileUploadResult complete(CompleteReferenceFileUploadCommand command) {
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        BidReferenceFile referenceFile = referenceFileRepository
                .findByIdAndCompanyId(command.referenceFileId(), companyId)
                .orElseThrow(() -> new NotFoundException(
                        BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_NOT_FOUND
                ));

        LocalDateTime now = LocalDateTime.now(clock);

        if (referenceFile.uploadStatus() != ReferenceFileUploadStatus.UPLOADING) {
            throw new ConflictException(
                    BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_NOT_UPLOADING
            );
        }
        if (referenceFile.uploadExpiresAt() != null
                && now.isAfter(referenceFile.uploadExpiresAt())) {
            failureRecorder.markUploadFailed(referenceFile, now);
            throw new ConflictException(
                    BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_UPLOAD_EXPIRED
            );
        }

        FileStoragePort.StoredObject stored = fileStoragePort.head(referenceFile.storageKey())
                .orElse(null);
        if (stored == null) {
            failureRecorder.markUploadFailed(referenceFile, now);
            throw new ConflictException(
                    BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_OBJECT_NOT_FOUND
            );
        }
        if (stored.sizeBytes() != referenceFile.sizeBytes()) {
            failureRecorder.markUploadFailed(referenceFile, now);
            throw new ConflictException(
                    BidReferenceFileErrorCode.BIDDING_REFERENCE_FILE_SIZE_MISMATCH
            );
        }

        BidReferenceFile completed = referenceFileRepository
                .saveCompletedWithIndexOutbox(referenceFile.completeUpload(now));

        return new CompleteReferenceFileUploadResult(
                completed.referenceFileId(),
                completed.fileName(),
                completed.uploadStatus().name(),
                completed.indexStatus().name(),
                completed.completedAt()
        );
    }
}