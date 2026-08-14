package com.group3.vitamins.bidding.projectconversion.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewFilePromotionPort;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.projectconversion.application.command.ConvertNoticeToProjectCommand;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeProjectAccessPort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeProjectExistencePort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeSummaryProjectLinkPort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidReviewProjectLinkPort;
import com.group3.vitamins.bidding.projectconversion.application.result.ConvertNoticeToProjectResult;
import com.group3.vitamins.bidding.projectconversion.application.usecase.ConvertNoticeToProjectUseCase;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.command.AddMemberCommand;
import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.result.ProjectResult;
import com.group3.vitamins.project.application.usecase.ProjectCommandUseCase;
import com.group3.vitamins.project.application.usecase.ProjectMemberCommandUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 공고 프로젝트 전환 오케스트레이션 (bid.md "공고 프로젝트 전환" §서버 처리).
 *
 * <p>1~13번까지 구현됨(6번은 별도 사전 체크 없이 11~12번의 {@code addMember} 내부 검증 +
 * 트랜잭션 롤백으로 충족 - 아래 8번 주석 참고. 13번은 별도 구현 없이 확정 종료 - 아래 13번 주석 참고).
 */
@Service
@RequiredArgsConstructor
public class ConvertNoticeToProjectService implements ConvertNoticeToProjectUseCase {

    private static final String COMPLETED_REVIEW_STATUS = "COMPLETED";

    // 추가 참여자 기본 권한 - 스펙에 값이 명시돼 있지 않아 임시로 EDITOR로 둠. 다음 단계에서 확정 필요.
    private static final String DEFAULT_MEMBER_PERMISSION = "EDITOR";

    private final BidNoticeProjectAccessPort noticeAccessPort;
    private final BidReviewProjectLinkPort reviewLinkPort;
    private final BidNoticeSummaryProjectLinkPort summaryLinkPort;
    private final BidNoticeProjectExistencePort noticeProjectExistencePort;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final ProjectCommandUseCase projectCommandUseCase;
    private final ProjectMemberCommandUseCase projectMemberCommandUseCase;
    private final Clock clock;
    private final BidReviewFilePromotionPort filePromotionPort;
    private final FileStoragePort fileStoragePort;

    @Override
    @Transactional
    public ConvertNoticeToProjectResult convert(ConvertNoticeToProjectCommand command) {
        // 1: 공고 존재 여부와 현재 회사의 접근 권한 확인. DISMISSED된 공고는 접근 불가로 처리한다
        //    (2026-08-13 결정 - bidreview의 검토 생성 접근 판정과 일관성을 맞춤).
        Long companyId = currentCompanyIdProvider.currentCompanyId();
        if (!noticeAccessPort.isAccessible(companyId, command.noticeId())) {
            throw new NotFoundException(BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND);
        }

        // 2: reviewId가 같은 공고·회사·요청자의 COMPLETED 검토인지 확인한다. ABANDONED·EXPIRED는
        //    review_status 자체가 COMPLETED와 다른 값이라, 상태값 하나만 봐도 "완료·미만료·미포기"가 같이 확인된다.
        validateReview(command, companyId);

        // 3: summaryId가 있으면(선택) 같은 공고·회사의 확정 요약이며 아직 다른 프로젝트에 연결되지
        //    않았는지 확인한다. confirmed=true는 이미 COMPLETED를 전제로만 켜지는 값이라(확정 액션 자체가
        //    COMPLETED 여부를 먼저 검사) confirmed만 봐도 "COMPLETED·확정"이 같이 확인된다.
        if (command.summaryId() != null) {
            validateSummary(command, companyId);
        }

        // 4: 이 공고로 이미 만든 프로젝트가 있는지 먼저 확인해서, 이미 전환된 공고면 5~6번(권한·초대자
        //    검증)까지 갈 필요 없이 빠르게 409로 끝낸다. 동시 요청 경합은 project 도메인의
        //    DB UNIQUE 제약 + createProject 내부 checkBidNoticeNotLinked가 최종적으로 막는다.
        if (noticeProjectExistencePort.existsForNotice(companyId, command.noticeId())) {
            throw new ConflictException(ProjectErrorCode.PROJECT_BID_NOTICE_ALREADY_LINKED);
        }

        // 5: 요청자가 BIDDING 권한을 갖는지 확인한다. bidnotice·bidsummary·bidreview·collectioncondition이
        //    이미 다 같이 쓰는 공용 정책을 그대로 재사용 - 새 포트·새 에러코드 없음.
        biddingAccessPolicy.assertAccess(command.requesterUserId(), command.role());

        // 6: memberIds가 초대 가능한 사용자이며 등록 가능한지는 별도 사전 체크를 안 만든다 -
        //    11~12번의 addMember 호출이 이미 EmployeeLookupPort로 "존재하는(삭제되지 않은) 같은 회사
        //    사원인지"를 확인하고(타사 사번은 findNameByUserId가 null을 반환 - EmployeeLookupAdapter가
        //    이미 companyId로 스코프함), 중복 등록도 확인한다. 실패하면 RuntimeException이 올라와
        //    이 메서드의 @Transactional이 지금까지의 프로젝트 생성(8번)까지 통째로 롤백한다 -
        //    "불완전한 프로젝트를 남기지 않는다"는 트랜잭션 정책과 정확히 일치한다.

        // 8: 프로젝트 생성 + project.bid_notice_id 저장 + 요청자 자동 등록(EDITOR)까지 한 번에 처리됨.
        // ⚠️ 4번의 선확인과 이 save() 사이엔 짧은 경합 창이 있다 - 두 요청이 동시에 4번을 통과하면
        // DB의 UNIQUE(bid_notice_id, company_id) 제약이 최종적으로 막아주지만, 그 위반은 도메인
        // 에러코드 없는 DataIntegrityViolationException(Spring 예외)으로 그대로 올라온다.
        // createProject는 project·project_member·project_business_category를 함께 저장하므로
        // DataIntegrityViolationException이 전부 "이미 전환된 공고" 때문이라고 단정하면 안 된다 -
        // 잡은 뒤 noticeProjectExistencePort로 다시 확인해서, 진짜 그 경합이 맞을 때만 409로 변환하고
        // 아니면(다른 무결성 위반) 원인 그대로 다시 던진다.
        ProjectResult project;
        try {
            project = projectCommandUseCase.createProject(new CreateProjectCommand(
                    command.noticeId(),
                    command.name(),
                    command.description(),
                    null,
                    command.startedOn(),
                    command.endedOn(),
                    null,
                    List.of(command.businessCategoryId()),
                    command.requesterUserId()
            ));
        } catch (DataIntegrityViolationException integrityViolation) {
            if (noticeProjectExistencePort.existsForNotice(companyId, command.noticeId())) {
                throw new ConflictException(ProjectErrorCode.PROJECT_BID_NOTICE_ALREADY_LINKED, integrityViolation);
            }
            throw integrityViolation;
        }

        // 9: summaryId가 있으면 bid_notice_summary.project_id에 연결한다. 3번 확인과 이 쓰기
        //    사이에도 8번과 같은 경합 창이 있다 - linkProject가 조건부 UPDATE(WHERE project_id IS
        //    NULL)라 영향받은 행이 0이면 그 사이 다른 요청이 먼저 연결한 것이므로 409로 변환한다.
        if (command.summaryId() != null) {
            boolean linked = summaryLinkPort.linkProject(
                    companyId, command.noticeId(), command.summaryId(),
                    project.projectId(), LocalDateTime.now(clock)
            );
            if (!linked) {
                throw new ConflictException(BiddingErrorCode.BIDDING_SUMMARY_ALREADY_LINKED);
            }
        }

        // 10: reviewId에서 실제 다운로드에 성공한 공고 첨부(BID_ATTACHMENT + READY)만 정식 파일로
        //     귀속한다. 사내 기준자료·사내 문서함 참조는 Worker 다운로드를 거치지 않아 대상이 아니다.
        //     bidReviewDocumentId를 파일 도메인 멱등키로 넘기므로(BidReviewFilePromotionAdapter)
        //     재시도해도 파일이 중복 생성되지 않는다.
        List<BidReviewProjectLinkPort.PromotableDocument> promotableDocuments =
                reviewLinkPort.findPromotableDocuments(command.reviewId());
        LocalDateTime promotionNow = LocalDateTime.now(clock);
        for (BidReviewProjectLinkPort.PromotableDocument document : promotableDocuments) {
            BidReviewFilePromotionPort.PromotedFile promoted = filePromotionPort.promote(
                    new BidReviewFilePromotionPort.PromotionRequest(
                            companyId,
                            project.projectId(),
                            command.requesterUserId(),
                            document.reviewDocumentId(),
                            document.temporaryStorageKey(),
                            document.fileName(),
                            document.fileSize()
                    )
            );

            boolean promotionRecorded = reviewLinkPort.markDocumentPromoted(
                    document.reviewDocumentId(), promoted.fileId(), promoted.fileVersionId(), promotionNow
            );
            if (!promotionRecorded) {
                throw new IllegalStateException("귀속하려는 검토 문서 상태가 예상과 다릅니다.");
            }
        }

        // ⚠️ 커밋 전에 지우면 안 된다 - 이 아래(review 연결·11~12번 addMember)에서 실패해 트랜잭션이
        // 롤백되면 문서는 READY로 되돌아가는데 실제 객체는 이미 삭제돼 재시도 시 귀속할 원본이 없어진다.
        // afterCommit 훅으로 미뤄서, 커밋에 실제로 성공했을 때만 지운다. deleteObjects 자체는 실패해도
        // 예외를 던지지 않으므로(FileStoragePort 계약) 여기서 지우지 못해도 데이터 정합성 문제는 아니다.
        if (!promotableDocuments.isEmpty()) {
            List<String> temporaryStorageKeys = promotableDocuments.stream()
                    .map(BidReviewProjectLinkPort.PromotableDocument::temporaryStorageKey)
                    .toList();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStoragePort.deleteObjects(temporaryStorageKeys);
                }
            });
        }

        boolean reviewLinked = reviewLinkPort.linkProject(command.reviewId(), project.projectId(), promotionNow);
        if (!reviewLinked) {
            throw new ConflictException(BidReviewErrorCode.BIDDING_REVIEW_ALREADY_LINKED_TO_PROJECT);
        }

        // 11~12: 추가 참여자 등록. 요청자는 8번에서 이미 등록됐으므로 겹치면 제외한다("중복 참여자" 규칙).
        for (String memberId : distinctAdditionalMembers(command)) {
            projectMemberCommandUseCase.addMember(new AddMemberCommand(
                    project.projectId(),
                    memberId,
                    DEFAULT_MEMBER_PERMISSION,
                    command.requesterUserId(),
                    command.role()
            ));
        }

        // 13: 기본 스테이지·스텝은 자동 생성하지 않기로 확정(2026-08-14) - project 도메인 자체가
        //     스테이지·스텝 없이 생성되는 것을 정상 상태로 취급해, 입찰 전환도 동일하게 따른다.
        //     "필요 시"라는 스펙 조건이 성립하지 않는 것으로 판단 - bid.md §13 참고. 별도 구현 없음.

        return new ConvertNoticeToProjectResult(project.projectId());
    }

    private void validateReview(ConvertNoticeToProjectCommand command, Long companyId) {
        BidReviewProjectLinkPort.ReviewSnapshot review = reviewLinkPort.findReview(command.reviewId())
                .orElseThrow(() -> new NotFoundException(BidReviewErrorCode.BIDDING_REVIEW_NOT_FOUND));

        if (!review.companyId().equals(companyId)
                || !review.noticeId().equals(command.noticeId())
                || !review.requestedBy().equals(command.requesterUserId())) {
            throw new ForbiddenException(BidReviewErrorCode.BIDDING_REVIEW_ACCESS_DENIED);
        }

        if (!COMPLETED_REVIEW_STATUS.equals(review.reviewStatus())) {
            throw new ConflictException(BidReviewErrorCode.BIDDING_REVIEW_NOT_COMPLETED);
        }
    }

    private void validateSummary(ConvertNoticeToProjectCommand command, Long companyId) {
        BidNoticeSummaryProjectLinkPort.SummarySnapshot summary = summaryLinkPort
                .findSummary(companyId, command.noticeId(), command.summaryId())
                .orElseThrow(() -> new NotFoundException(BiddingErrorCode.BIDDING_SUMMARY_NOT_FOUND));

        if (!summary.confirmed()) {
            throw new ConflictException(BiddingErrorCode.BIDDING_SUMMARY_NOT_CONFIRMED);
        }
        if (summary.projectId() != null) {
            throw new ConflictException(BiddingErrorCode.BIDDING_SUMMARY_ALREADY_LINKED);
        }
    }

    private List<String> distinctAdditionalMembers(ConvertNoticeToProjectCommand command) {
        return command.memberIds().stream()
                .distinct()
                .filter(memberId -> !memberId.equals(command.requesterUserId()))
                .toList();
    }
}
