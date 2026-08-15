package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCompanyDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewQualificationPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewReferenceFilePort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewWorkerPort;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewJobQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewJobResult;
import com.group3.vitamins.bidding.bidreview.application.usecase.GetBidReviewJobUseCase;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.group3.vitamins.bidding.bidreview.application.support.BidReviewAttemptIdValidator.isValid;

@Service
@RequiredArgsConstructor
public class BidReviewJobQueryService implements GetBidReviewJobUseCase {

    private static final String BID_ATTACHMENT = "BID_ATTACHMENT";
    private static final String INTERNAL_REFERENCE = "INTERNAL_REFERENCE";
    private static final String COMPANY_DOCUMENT_REFERENCE = "COMPANY_DOCUMENT_REFERENCE";

    // 공고 첨부 임시 업로드 presign 전용 - 실제 파일 형식과 무관한 자리표시자다. Worker는 업로드 PUT에
    // 반드시 이 값을 Content-Type으로 그대로 보내야 한다(서명에 포함돼 있어 값이 다르면 403).
    private static final String ATTACHMENT_UPLOAD_CONTENT_TYPE = "application/octet-stream";

    private final BidReviewWorkerPort workerPort;
    private final BidReviewNoticeDocumentPort noticeDocumentPort;
    private final BidReviewReferenceFilePort referenceFilePort;
    private final BidReviewCompanyDocumentPort companyDocumentPort;
    private final BidReviewQualificationPort qualificationPort;
    private final FileStoragePort fileStoragePort;
    private final Clock clock;

    @Override
    @Transactional
    public BidReviewJobResult handle(GetBidReviewJobQuery query) {
        validate(query);

        BidReviewWorkerPort.ClaimedJob job = workerPort.claimJob(
                query.reviewId(),
                query.attemptId(),
                LocalDateTime.now(clock)
        ).orElseThrow(() -> new NotFoundException(
                BidReviewErrorCode.BIDDING_REVIEW_JOB_NOT_FOUND
        ));

        String noticeName = noticeDocumentPort
                .findAccessibleNotice(job.companyId(), job.noticeId())
                .map(BidReviewNoticeDocumentPort.NoticeSnapshot::noticeName)
                // 검토 생성 시점에 이미 접근 가능한 공고만 저장되므로 이론상 도달하지 않는다.
                // 방어적으로만 남겨둔다.
                .orElseThrow(() -> new NotFoundException(
                        BidReviewErrorCode.BIDDING_REVIEW_JOB_NOT_FOUND
                ));

        List<Long> attachmentIds = job.documents().stream()
                .filter(document -> BID_ATTACHMENT.equals(document.documentRole()))
                .map(BidReviewWorkerPort.JobDocument::bidAttachmentId)
                .toList();

        List<Long> referenceFileIds = job.documents().stream()
                .filter(document -> INTERNAL_REFERENCE.equals(document.documentRole()))
                .map(BidReviewWorkerPort.JobDocument::referenceFileId)
                .toList();

        List<Long> companyDocumentVersionIds = job.documents().stream()
                .filter(document -> COMPANY_DOCUMENT_REFERENCE.equals(document.documentRole()))
                .map(BidReviewWorkerPort.JobDocument::companyDocumentVersionId)
                .toList();

        Map<Long, BidReviewNoticeDocumentPort.AttachmentSnapshot> attachmentsById =
                noticeDocumentPort
                        .findAttachments(job.companyId(), job.noticeId(), attachmentIds)
                        .stream()
                        .collect(Collectors.toMap(
                                BidReviewNoticeDocumentPort.AttachmentSnapshot::attachmentId,
                                Function.identity()
                        ));

        Map<Long, BidReviewReferenceFilePort.DownloadableReferenceFile> referenceFilesById =
                referenceFilePort
                        .findDownloadableFiles(job.companyId(), referenceFileIds)
                        .stream()
                        .collect(Collectors.toMap(
                                BidReviewReferenceFilePort.DownloadableReferenceFile::referenceFileId,
                                Function.identity()
                        ));

        Map<Long, BidReviewCompanyDocumentPort.DownloadableCompanyDocument> companyDocumentsById =
                companyDocumentPort
                        .findDownloadableDocuments(job.companyId(), companyDocumentVersionIds)
                        .stream()
                        .collect(Collectors.toMap(
                                BidReviewCompanyDocumentPort.DownloadableCompanyDocument::companyDocumentVersionId,
                                Function.identity()
                        ));

        List<BidReviewJobResult.AttachmentJob> attachments = attachmentIds.stream()
                .map(attachmentsById::get)
                .filter(Objects::nonNull)
                .map(attachment -> toAttachmentJob(job.companyId(), job.reviewId(), attachment))
                .toList();

        List<BidReviewJobResult.ReferenceFileJob> referenceFiles = referenceFileIds.stream()
                .map(referenceFilesById::get)
                .filter(Objects::nonNull)
                .map(reference -> new BidReviewJobResult.ReferenceFileJob(
                        reference.referenceFileId(),
                        reference.fileName(),
                        reference.downloadUrl()
                ))
                .toList();

        List<BidReviewJobResult.CompanyDocumentJob> companyDocuments = companyDocumentVersionIds.stream()
                .map(companyDocumentsById::get)
                .filter(Objects::nonNull)
                .map(document -> new BidReviewJobResult.CompanyDocumentJob(
                        document.companyDocumentVersionId(),
                        document.fileName(),
                        document.downloadUrl()
                ))
                .toList();

        String qualificationSummary = formatQualificationSummary(job.companyId());

        return new BidReviewJobResult(
                job.reviewId(),
                job.companyId(),
                job.attemptId(),
                job.prompt(),
                job.noticeId(),
                noticeName,
                attachments,
                referenceFiles,
                companyDocuments,
                qualificationSummary
        );
    }

    // 공고 첨부는 아직 우리 S3에 없는 외부 원본이라, Worker가 다운로드 후 되올릴 임시 업로드 URL을
    // 매번 새로 발급한다(재시도로 다시 조회돼도 무방 - 이 키는 이번 왕복에서만 쓰고 그대로 callback에
    // 되돌아온다). 실제 저장은 Worker가 지정한 대로 믿는다 - Spring은 URL만 발급하고 검증하지 않는다.
    // ⚠️ companies/{companyId}/ 접두사는 장식이 아니다 - 프로젝트 귀속 시 AttachStagedFileService가
    // 이 접두사로 테넌트 경계를 검증한다(requireTenantScopedTempKey). 빼면 귀속 호출이 전부 400으로 거절된다.
    private BidReviewJobResult.AttachmentJob toAttachmentJob(
            Long companyId,
            Long reviewId,
            BidReviewNoticeDocumentPort.AttachmentSnapshot attachment
    ) {
        String temporaryStorageKey = "companies/%d/bidding/reviews/%d/attachments/%d/%s".formatted(
                companyId, reviewId, attachment.attachmentId(), UUID.randomUUID()
        );
        String uploadUrl = fileStoragePort
                .presignUpload(temporaryStorageKey, ATTACHMENT_UPLOAD_CONTENT_TYPE, 0)
                .url();

        return new BidReviewJobResult.AttachmentJob(
                attachment.attachmentId(),
                attachment.fileName(),
                attachment.sourceUrl(),
                uploadUrl,
                temporaryStorageKey
        );
    }

    // 개인 식별 정보 없이 인원수만 담은 텍스트를 만든다. 세 집계는 교차하지 않고 독립적으로 낸다
    // (전공×학력처럼 교차하면 인원수가 줄어 개인 특정 위험이 커짐 - BidReviewQualificationPort 참고).
    private String formatQualificationSummary(Long companyId) {
        String majors = formatCounts(qualificationPort.summarizeMajors(companyId));
        String degrees = formatCounts(qualificationPort.summarizeDegrees(companyId));
        String certificates = formatCounts(qualificationPort.summarizeCertificates(companyId));

        return "[보유 전공 현황(재직 중)]\n" + majors
                + "\n\n[보유 학력 현황(재직 중)]\n" + degrees
                + "\n\n[보유 자격증 현황(재직 중)]\n" + certificates;
    }

    private String formatCounts(List<BidReviewQualificationPort.NameCount> counts) {
        if (counts.isEmpty()) {
            return "등록된 정보 없음";
        }

        return counts.stream()
                .map(count -> count.name() + " " + count.headcount() + "명")
                .collect(Collectors.joining(", "));
    }

    private void validate(GetBidReviewJobQuery query) {
        if (query == null
                || query.reviewId() == null
                || query.reviewId() <= 0
                || !isValid(query.attemptId())) {
            throw new ValidationException(
                    BidReviewErrorCode.BIDDING_INVALID_REVIEW_REQUEST
            );
        }
    }
}
