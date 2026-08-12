package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.referencefile.application.command.StartReferenceFileUploadCommand;
import com.group3.vitamins.bidding.referencefile.application.result.StartReferenceFileUploadResult;
import com.group3.vitamins.bidding.referencefile.application.usecase.StartReferenceFileUploadUseCase;
import com.group3.vitamins.bidding.referencefile.domain.exception.BidReferenceFileErrorCode;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class StartReferenceFileUploadService implements StartReferenceFileUploadUseCase {

    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;
    private static final int MAX_EXTENSION_LENGTH = 20;

    private final BidReferenceFileRepository referenceFileRepository;
    private final FileStoragePort fileStoragePort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    @Override
    public StartReferenceFileUploadResult start(StartReferenceFileUploadCommand command) {
        biddingAccessPolicy.assertAccess(command.userId(), command.role());
        validate(command);

        LocalDateTime now = LocalDateTime.now(clock);
        Long companyId = currentCompanyIdProvider.currentCompanyId();
        String extension = extractExtension(command.fileName());
        String storageKey = buildStorageKey(companyId, extension);

        BidReferenceFile created = referenceFileRepository.save(
                BidReferenceFile.createUploading(
                        companyId,
                        command.fileName(),
                        extension,
                        command.mimeType(),
                        command.sizeBytes(),
                        storageKey,
                        command.userId(),
                        now.plusMinutes(10),
                        now
                )
        );

        FileStoragePort.PresignedUrl presigned = fileStoragePort.presignUpload(
                storageKey, command.mimeType(), command.sizeBytes()
        );

        return new StartReferenceFileUploadResult(
                created.referenceFileId(),
                presigned.url(),
                LocalDateTime.ofInstant(presigned.expiresAt(), clock.getZone())
        );
    }

    private void validate(StartReferenceFileUploadCommand command) {
        if (command.fileName() == null || command.fileName().isBlank()
                || command.fileName().length() > 255
                || command.mimeType() == null || command.mimeType().isBlank()
                || command.mimeType().length() > 100
                || command.sizeBytes() <= 0
                || command.sizeBytes() > MAX_SIZE_BYTES) {
            throw new ValidationException(
                    BidReferenceFileErrorCode.BIDDING_INVALID_REFERENCE_FILE_REQUEST
            );
        }
    }

    // 마지막 '.' 뒤 문자열을 그대로 storageKey에 넣지 않는다 — 영숫자·길이 제한을 벗어나면
    // storageKey 구조가 깨질 수 있어 확장자 없음으로 취급한다.
    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }

        String candidate = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        boolean isSafe = candidate.length() <= MAX_EXTENSION_LENGTH
                && candidate.chars().allMatch(Character::isLetterOrDigit);

        return isSafe ? candidate : "";
    }

    private String buildStorageKey(Long companyId, String extension) {
        String uuid = UUID.randomUUID().toString();
        String suffix = extension.isEmpty() ? "" : "." + extension;
        return "companies/%d/bidding/reference-files/%s%s".formatted(companyId, uuid, suffix);
    }
}