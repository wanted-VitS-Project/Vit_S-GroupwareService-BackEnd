package com.group3.vitamins.bidding.projectconversion.application.service;

import com.group3.vitamins.bidding.projectconversion.application.command.ConvertNoticeToProjectCommand;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeProjectAccessPort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeProjectExistencePort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeSummaryProjectLinkPort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidReviewProjectLinkPort;
import com.group3.vitamins.bidding.projectconversion.application.result.ConvertNoticeToProjectResult;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.project.application.port.BlockClonePort;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.port.StageCascadePort;
import com.group3.vitamins.project.application.port.StageClonePort;
import com.group3.vitamins.project.application.port.StepCascadePort;
import com.group3.vitamins.project.application.port.StepClonePort;
import com.group3.vitamins.project.application.port.StepPermissionCleanupPort;
import com.group3.vitamins.project.application.port.StagePermissionDefaultCleanupPort;
import com.group3.vitamins.project.application.port.StepStatLookupPort;
import com.group3.vitamins.project.application.service.ProjectAccessService;
import com.group3.vitamins.project.application.service.ProjectCommandService;
import com.group3.vitamins.project.application.service.ProjectMemberCommandService;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.infrastructure.persistence.ProjectBusinessCategoryRepositoryAdapter;
import com.group3.vitamins.project.infrastructure.persistence.ProjectMemberRepositoryAdapter;
import com.group3.vitamins.project.infrastructure.persistence.ProjectRepositoryAdapter;
import com.group3.vitamins.project.infrastructure.persistence.SpringDataProjectMemberRepository;
import com.group3.vitamins.project.infrastructure.persistence.SpringDataProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 실제 DB 트랜잭션으로 "프로젝트 생성 + 같은 트랜잭션 안에서 추가 참여자 등록"이 막히지 않는지 확인한다.
 *
 * <p>{@code ConvertNoticeToProjectService.convert()}는 {@code createProject}(요청자를 EDITOR로
 * 자동 등록)와 {@code addMember}(호출자의 편집 권한을 {@code requireEditable}로 확인)를 <b>같은
 * {@code @Transactional} 안에서</b> 순서대로 부른다. {@code addMember}의 권한 확인이 방금 커밋되지
 * 않은 요청자의 EDITOR 등록을 못 보고 거부하지는 않는지가 이 테스트의 핵심 질문이다.
 *
 * <p>{@code role}을 ADMIN/MASTER(전역 관리자, 권한 체크 자체를 건너뜀)가 아닌 일반 역할("MEMBER")로
 * 둬서 진짜 참여자 권한 조회 경로를 타게 한다 - 안 그러면 이 테스트가 아무것도 검증하지 않게 된다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:convert-notice-to-project;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        ProjectRepositoryAdapter.class,
        ProjectMemberRepositoryAdapter.class,
        ProjectBusinessCategoryRepositoryAdapter.class,
        ProjectAccessPolicy.class,
        ProjectAccessService.class,
        ProjectCommandService.class,
        ProjectMemberCommandService.class,
        ConvertNoticeToProjectService.class,
        ConvertNoticeToProjectIntegrationTest.TestConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("공고 프로젝트 전환 - 생성 직후 추가 참여자 등록(addMember) 실제 트랜잭션 검증")
class ConvertNoticeToProjectIntegrationTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 100L;
    private static final Long REVIEW_ID = 1L;
    // 두 번째 테스트(6번 - 존재하지 않는 초대자로 인한 롤백) 전용 - 첫 번째 테스트가 만든 프로젝트와
    // 절대 겹치지 않게 별도 noticeId/reviewId를 쓴다. 이 클래스는 @Transactional(NOT_SUPPORTED)라
    // 테스트 메서드 사이에 롤백이 안 되고 같은 H2 인스턴스를 공유하기 때문에 이렇게 분리해야 한다.
    private static final Long NOTICE_ID_FOR_ROLLBACK_TEST = 200L;
    private static final Long REVIEW_ID_FOR_ROLLBACK_TEST = 2L;
    private static final Long BUSINESS_CATEGORY_ID = 500L;
    private static final String REQUESTER_USER_ID = "EMP001";
    private static final String ADDITIONAL_MEMBER_USER_ID = "EMP002";
    private static final String NONEXISTENT_MEMBER_USER_ID = "EMP999";

    @Autowired
    private ConvertNoticeToProjectService convertNoticeToProjectService;

    @Autowired
    private SpringDataProjectMemberRepository projectMemberRepository;

    @Autowired
    private SpringDataProjectRepository projectRepository;

    @Test
    @DisplayName("createProject 직후 같은 트랜잭션에서 addMember를 호출해도 편집 권한 확인이 통과한다")
    void addsMemberRightAfterCreatingProjectInSameTransaction() {
        ConvertNoticeToProjectCommand command = new ConvertNoticeToProjectCommand(
                NOTICE_ID,
                REVIEW_ID,
                null,                       // summaryId
                "테스트 프로젝트",
                null,
                BUSINESS_CATEGORY_ID,
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 12, 31),
                List.of(ADDITIONAL_MEMBER_USER_ID),
                REQUESTER_USER_ID,
                "MEMBER"                    // ⚠️ 전역 관리자가 아닌 일반 역할이어야 requireEditable이 실제로 검사한다
        );

        ConvertNoticeToProjectResult result = convertNoticeToProjectService.convert(command);

        assertThat(result.projectId()).isNotNull();

        Optional<MemberPermission> requesterPermission = projectMemberRepository
                .findByProjectIdAndUserId(result.projectId(), REQUESTER_USER_ID)
                .map(entity -> entity.getPermission());
        Optional<MemberPermission> additionalMemberPermission = projectMemberRepository
                .findByProjectIdAndUserId(result.projectId(), ADDITIONAL_MEMBER_USER_ID)
                .map(entity -> entity.getPermission());

        assertThat(requesterPermission).contains(MemberPermission.EDITOR);
        assertThat(additionalMemberPermission).contains(MemberPermission.EDITOR);
    }

    @Test
    @DisplayName("6번: 존재하지 않는 추가 참여자가 섞여 있으면 addMember에서 실패하고 방금 만든 프로젝트까지 통째로 롤백된다")
    void rollsBackEntireConversionWhenAdditionalMemberDoesNotExist() {
        ConvertNoticeToProjectCommand command = new ConvertNoticeToProjectCommand(
                NOTICE_ID_FOR_ROLLBACK_TEST,
                REVIEW_ID_FOR_ROLLBACK_TEST,
                null,
                "롤백 테스트 프로젝트",
                null,
                BUSINESS_CATEGORY_ID,
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 12, 31),
                List.of(NONEXISTENT_MEMBER_USER_ID),
                REQUESTER_USER_ID,
                "MEMBER"
        );

        assertThatThrownBy(() -> convertNoticeToProjectService.convert(command))
                .isInstanceOf(com.group3.vitamins.global.domain.common.error.exception.NotFoundException.class);

        assertThat(projectRepository.findByBidNoticeIdAndCompanyIdAndDeletedAtIsNull(
                NOTICE_ID_FOR_ROLLBACK_TEST, COMPANY_ID
        )).isEmpty();
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        CurrentCompanyIdProvider currentCompanyIdProvider() {
            CurrentCompanyIdProvider provider = mock(CurrentCompanyIdProvider.class);
            when(provider.currentCompanyId()).thenReturn(COMPANY_ID);
            return provider;
        }

        @Bean
        BidNoticeProjectAccessPort bidNoticeProjectAccessPort() {
            BidNoticeProjectAccessPort port = mock(BidNoticeProjectAccessPort.class);
            when(port.isAccessible(COMPANY_ID, NOTICE_ID)).thenReturn(true);
            when(port.isAccessible(COMPANY_ID, NOTICE_ID_FOR_ROLLBACK_TEST)).thenReturn(true);
            return port;
        }

        @Bean
        BidReviewProjectLinkPort bidReviewProjectLinkPort() {
            BidReviewProjectLinkPort port = mock(BidReviewProjectLinkPort.class);
            when(port.findReview(REVIEW_ID)).thenReturn(Optional.of(
                    new BidReviewProjectLinkPort.ReviewSnapshot(
                            REVIEW_ID, COMPANY_ID, NOTICE_ID, REQUESTER_USER_ID, "COMPLETED", null
                    )
            ));
            when(port.findReview(REVIEW_ID_FOR_ROLLBACK_TEST)).thenReturn(Optional.of(
                    new BidReviewProjectLinkPort.ReviewSnapshot(
                            REVIEW_ID_FOR_ROLLBACK_TEST, COMPANY_ID, NOTICE_ID_FOR_ROLLBACK_TEST,
                            REQUESTER_USER_ID, "COMPLETED", null
                    )
            ));
            return port;
        }

        // 이 통합 테스트는 summaryId=null로 호출해서 3단계 검증 자체를 안 타므로 스텁 없이 빈만 채운다.
        @Bean
        BidNoticeSummaryProjectLinkPort bidNoticeSummaryProjectLinkPort() {
            return mock(BidNoticeSummaryProjectLinkPort.class);
        }

        @Bean
        BidNoticeProjectExistencePort bidNoticeProjectExistencePort() {
            BidNoticeProjectExistencePort port = mock(BidNoticeProjectExistencePort.class);
            when(port.existsForNotice(COMPANY_ID, NOTICE_ID)).thenReturn(false);
            when(port.existsForNotice(COMPANY_ID, NOTICE_ID_FOR_ROLLBACK_TEST)).thenReturn(false);
            return port;
        }

        // assertAccess는 void라 스텁 없이도 기본이 통과(no-op)다 - "MEMBER" 역할로 호출해도 막히지 않는다.
        @Bean
        BiddingAccessPolicy biddingAccessPolicy() {
            return mock(BiddingAccessPolicy.class);
        }

        @Bean
        EmployeeLookupPort employeeLookupPort() {
            EmployeeLookupPort port = mock(EmployeeLookupPort.class);
            when(port.findNameByUserId(REQUESTER_USER_ID)).thenReturn("김입찰");
            when(port.findNameByUserId(ADDITIONAL_MEMBER_USER_ID)).thenReturn("박멤버");
            return port;
        }

        @Bean
        BusinessCategoryLookupPort businessCategoryLookupPort() {
            BusinessCategoryLookupPort port = mock(BusinessCategoryLookupPort.class);
            when(port.findByIds(List.of(BUSINESS_CATEGORY_ID), COMPANY_ID))
                    .thenReturn(List.of(new BusinessCategoryLookupPort.BusinessCategoryView(
                            BUSINESS_CATEGORY_ID, "SW개발", "SW")));
            return port;
        }

        // addMember 경로에서 쓰지 않지만 ProjectMemberCommandService 생성자에 필요하다.
        @Bean
        StepPermissionCleanupPort stepPermissionCleanupPort() {
            return mock(StepPermissionCleanupPort.class);
        }

        @Bean
        StagePermissionDefaultCleanupPort stagePermissionDefaultCleanupPort() {
            return mock(StagePermissionDefaultCleanupPort.class);
        }

        // createProject 경로에서 쓰지 않지만 ProjectCommandService 생성자에 필요하다.
        @Bean
        StepStatLookupPort stepStatLookupPort() {
            return mock(StepStatLookupPort.class);
        }

        @Bean
        StageCascadePort stageCascadePort() {
            return mock(StageCascadePort.class);
        }

        @Bean
        StepCascadePort stepCascadePort() {
            return mock(StepCascadePort.class);
        }

        // 복제(PRJ-018) 전용 포트 3종. 공고 전환은 createProject 만 타므로 호출되지 않는다.
        @Bean
        StageClonePort stageClonePort() {
            return mock(StageClonePort.class);
        }

        @Bean
        StepClonePort stepClonePort() {
            return mock(StepClonePort.class);
        }

        @Bean
        BlockClonePort blockClonePort() {
            return mock(BlockClonePort.class);
        }

        // 참여자 추가 시 PROJECT_INVITED 알림을 발행한다. 이 테스트는 트랜잭션 전파만 보므로 목으로 둔다.
        @Bean
        DomainEventPublisher domainEventPublisher() {
            return mock(DomainEventPublisher.class);
        }
    }
}
