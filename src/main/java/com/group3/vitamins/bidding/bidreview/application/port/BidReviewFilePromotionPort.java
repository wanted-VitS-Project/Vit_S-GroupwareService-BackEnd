package com.group3.vitamins.bidding.bidreview.application.port;

// 검토에서 실제 다운로드된 공고 첨부를 프로젝트 정식 파일로 귀속(승격)한다 - 김동현의 파일 도메인
// AttachStagedFileUseCase(FILE-V1 §2-G, 입찰 승격 전용 in-process 포트)를 감싼다.
// ⚠️ 임시 S3 객체 삭제는 이 포트의 책임이 아니다(PROMOTE-008) - 귀속 성공 후 호출자가 별도로 정리한다.
public interface BidReviewFilePromotionPort {

    PromotedFile promote(PromotionRequest request);

    record PromotionRequest(
            Long companyId,
            Long projectId,
            String requesterUserId,
            Long bidReviewDocumentId,
            String temporaryStorageKey,
            String fileName,
            long fileSizeBytes
    ) {
    }

    record PromotedFile(Long fileId, Long fileVersionId) {
    }
}
