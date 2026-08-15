package com.group3.vitamins.bidding.collectioncondition.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.command.CreateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.application.command.UpdateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.*;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionConditionRepository;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionSourceRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("CollectionConditionService 수집 조건 관리")
class CollectionConditionServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final String USER_ID = "EMP001";

    private CollectionConditionRepository conditionRepository;
    private CollectionSourceRepository sourceRepository;
    private CurrentCompanyIdProvider companyIdProvider;
    private BiddingAccessPolicy biddingAccessPolicy;
    private CollectionConditionService service;

    @BeforeEach
    void setUp() {
        conditionRepository = mock(CollectionConditionRepository.class);
        sourceRepository = mock(CollectionSourceRepository.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);

        service = new CollectionConditionService(
                conditionRepository,
                sourceRepository,
                companyIdProvider,
                biddingAccessPolicy
        );
    }

    @Test
    @DisplayName("현재 회사 ID로 삭제되지 않은 수집 조건만 조회한다")
    void getsConditionsByCurrentCompany() {
        when(conditionRepository.findAllNotDeleted(COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.getAll(USER_ID, "ADMIN")).isEmpty();

        verify(biddingAccessPolicy).assertAccess(USER_ID, "ADMIN");
        verify(conditionRepository).findAllNotDeleted(COMPANY_ID);
    }

    @Test
    @DisplayName("현재 회사 소속으로 수집 조건을 등록한다")
    void createsConditionForCurrentCompany() {
        CollectionSource source =
                new CollectionSource(1L, "NARA", "나라장터", "OPEN_API", true);

        when(sourceRepository.findNotDeletedByCode("NARA"))
                .thenReturn(Optional.of(source));
        when(conditionRepository.save(any(CollectionCondition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(validCreateCommand());

        ArgumentCaptor<CollectionCondition> captor =
                ArgumentCaptor.forClass(CollectionCondition.class);

        verify(conditionRepository).save(captor.capture());

        CollectionCondition saved = captor.getValue();
        assertThat(saved.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(saved.getSourceCode()).isEqualTo("NARA");
        assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("UTC 환경의 주말에도 서울 시간 기준 다음 평일을 계산한다")
    void calculatesNextWeekdayUsingSeoulTimezone() {
        CollectionConditionService fixedService = serviceWithClock(
                Instant.parse("2026-08-14T23:30:00Z")
        );
        prepareSuccessfulSave();

        fixedService.create(scheduledCommand(
                CollectionScheduleType.WEEKDAYS, LocalTime.of(9, 0)
        ));

        assertThat(captureSavedCondition().getNextRunAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 17, 9, 0));
    }

    @Test
    @DisplayName("서울 예약 시각과 정확히 같으면 다음 회차를 계산한다")
    void advancesWhenCurrentTimeEqualsScheduledTime() {
        CollectionConditionService fixedService = serviceWithClock(
                Instant.parse("2026-08-10T00:00:00Z")
        );
        prepareSuccessfulSave();

        fixedService.create(scheduledCommand(
                CollectionScheduleType.DAILY, LocalTime.of(9, 0)
        ));

        assertThat(captureSavedCondition().getNextRunAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 11, 9, 0));
    }

    @Test
    @DisplayName("지원하지 않는 수집처면 등록을 거부한다")
    void rejectsUnsupportedSource() {
        when(sourceRepository.findNotDeletedByCode("NARA"))
                .thenReturn(Optional.empty());

        assertError(
                () -> service.create(validCreateCommand()),
                BiddingErrorCode.BIDDING_UNSUPPORTED_SOURCE
        );

        verify(conditionRepository, never()).save(any());
    }

    @Test
    @DisplayName("외부 API 호출 조합이 20개를 초과하면 등록을 거부한다")
    void rejectsTooManyQueryCombinations() {
        CollectionConditionFilter filters = new CollectionConditionFilter(
                List.of("키워드1", "키워드2"),
                List.of("11", "26", "41"),
                List.of("6201", "6202"),
                null,
                null,
                true,
                InternationalBidType.DOMESTIC
        );

        CreateCollectionConditionCommand command =
                new CreateCollectionConditionCommand(
                        "NARA",
                        "과다 호출 조건",
                        List.of(BidNoticeType.CONSTRUCTION, BidNoticeType.SERVICE),
                        filters,
                        true,
                        USER_ID,
                        "ADMIN"
                );

        assertError(
                () -> service.create(command),
                BiddingErrorCode.BIDDING_COLLECTION_QUERY_LIMIT_EXCEEDED
        );

        verify(sourceRepository, never()).findNotDeletedByCode(anyString());
        verify(conditionRepository, never()).save(any());
    }

    @Test
    @DisplayName("isActive가 없으면 잘못된 요청으로 거부한다")
    void rejectsMissingActive() {
        CreateCollectionConditionCommand command =
                new CreateCollectionConditionCommand(
                        "NARA",
                        "수집 조건",
                        List.of(BidNoticeType.SERVICE),
                        validFilter(),
                        null,
                        USER_ID,
                        "ADMIN"
                );

        assertError(
                () -> service.create(command),
                BiddingErrorCode.BIDDING_INVALID_COLLECTION_CONDITION
        );

        verify(conditionRepository, never()).save(any());
    }

    private CreateCollectionConditionCommand validCreateCommand() {
        return new CreateCollectionConditionCommand(
                "NARA",
                "수도권 스마트시티 용역",
                List.of(BidNoticeType.SERVICE),
                validFilter(),
                true,
                USER_ID,
                "ADMIN"
        );
    }

    private CreateCollectionConditionCommand scheduledCommand(
            CollectionScheduleType scheduleType,
            LocalTime scheduledTime
    ) {
        return new CreateCollectionConditionCommand(
                "NARA", "자동 수집 조건", List.of(BidNoticeType.SERVICE),
                validFilter(), null, true, true, scheduleType, scheduledTime,
                "Asia/Seoul", USER_ID, "ADMIN"
        );
    }

    private CollectionConditionService serviceWithClock(Instant instant) {
        return new CollectionConditionService(
                conditionRepository, sourceRepository, companyIdProvider,
                biddingAccessPolicy, Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    private void prepareSuccessfulSave() {
        when(sourceRepository.findNotDeletedByCode("NARA"))
                .thenReturn(Optional.of(new CollectionSource(
                        1L, "NARA", "나라장터", "OPEN_API", true
                )));
        when(conditionRepository.save(any(CollectionCondition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CollectionCondition captureSavedCondition() {
        ArgumentCaptor<CollectionCondition> captor =
                ArgumentCaptor.forClass(CollectionCondition.class);
        verify(conditionRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("공백 키워드가 포함되면 등록을 거부한다")
    void rejectsBlankKeyword() {
        CollectionConditionFilter filter = new CollectionConditionFilter(
                List.of(" "), List.of("11"), List.of("6202"),
                null, null, true, InternationalBidType.DOMESTIC
        );

        assertError(
                () -> service.create(new CreateCollectionConditionCommand(
                        "NARA", "수집 조건", List.of(BidNoticeType.SERVICE),
                        filter, true, USER_ID, "ADMIN"
                )),
                BiddingErrorCode.BIDDING_INVALID_COLLECTION_CONDITION
        );

        verify(conditionRepository, never()).save(any());
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 조건 등록을 거부한다")
    void rejectsCreateWithoutBiddingAccess() {
        doThrow(new ForbiddenException(
                BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED
        )).when(biddingAccessPolicy).assertAccess(USER_ID, "MEMBER");

        CreateCollectionConditionCommand command = new CreateCollectionConditionCommand(
                "NARA", "수집 조건", List.of(BidNoticeType.SERVICE),
                validFilter(), true, USER_ID, "MEMBER"
        );

        assertError(
                () -> service.create(command),
                BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED
        );
        verify(conditionRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 요청도 외부 API 호출 조합 제한을 적용한다")
    void rejectsUpdateWithTooManyQueryCombinations() {
        CollectionConditionFilter filters = new CollectionConditionFilter(
                List.of("키워드1", "키워드2"),
                List.of("11", "26", "41"),
                List.of("6201", "6202"),
                null, null, true, InternationalBidType.DOMESTIC
        );
        UpdateCollectionConditionCommand command = new UpdateCollectionConditionCommand(
                1L, "과다 호출 조건",
                List.of(BidNoticeType.CONSTRUCTION, BidNoticeType.SERVICE),
                filters, true, USER_ID, "ADMIN"
        );

        assertError(
                () -> service.update(command),
                BiddingErrorCode.BIDDING_COLLECTION_QUERY_LIMIT_EXCEEDED
        );
        verify(conditionRepository, never()).findNotDeletedById(anyLong(), anyLong());
    }

    @Test
    @DisplayName("현재 회사가 소유한 수집 조건의 내용을 전체 교체한다")
    void updatesOwnedCondition() {
        CollectionCondition condition = CollectionCondition.create(
                COMPANY_ID, "NARA", "기존 조건",
                List.of(BidNoticeType.CONSTRUCTION), validFilter(), true,
                USER_ID, java.time.LocalDateTime.now()
        );
        CollectionCondition restored = CollectionCondition.restore(
                1L, COMPANY_ID, condition.getSourceCode(), condition.getConditionName(),
                condition.getNoticeTypes(), condition.getFilters(), condition.isActive(),
                null, null, USER_ID, java.time.LocalDateTime.now(), null, null
        );
        CollectionConditionFilter replacement = new CollectionConditionFilter(
                List.of("교체 키워드"), List.of("26"), List.of("7101"),
                null, null, false, InternationalBidType.INTERNATIONAL
        );
        UpdateCollectionConditionCommand command = new UpdateCollectionConditionCommand(
                1L, "변경 조건", List.of(BidNoticeType.SERVICE), replacement,
                false, USER_ID, "ADMIN"
        );

        when(conditionRepository.findNotDeletedById(1L, COMPANY_ID))
                .thenReturn(Optional.of(restored));
        when(conditionRepository.save(any(CollectionCondition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(sourceRepository.findNotDeletedByCode("NARA"))
                .thenReturn(Optional.of(new CollectionSource(
                        1L, "NARA", "나라장터", "OPEN_API", true
                )));

        service.update(command);

        assertThat(restored.getConditionName()).isEqualTo("변경 조건");
        assertThat(restored.getNoticeTypes()).containsExactly(BidNoticeType.SERVICE);
        assertThat(restored.getFilters().keywords()).containsExactly("교체 키워드");
        assertThat(restored.isActive()).isFalse();
        verify(conditionRepository).findNotDeletedById(1L, COMPANY_ID);
    }

    private CollectionConditionFilter validFilter() {
        return new CollectionConditionFilter(
                List.of("스마트시티"),
                List.of("11"),
                List.of("6202"),
                100_000_000L,
                1_000_000_000L,
                true,
                InternationalBidType.DOMESTIC
        );
    }

    private void assertError(Runnable action, BiddingErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(exception ->
                        assertThat(((DomainException) exception).getErrorCode())
                                .isEqualTo(expected)
                );
    }
}
