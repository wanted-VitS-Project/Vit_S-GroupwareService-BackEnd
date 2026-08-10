package com.group3.vitamins.bidding.collectioncondition.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.command.CreateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.*;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionConditionRepository;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionSourceRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

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
    private CollectionConditionService service;

    @BeforeEach
    void setUp() {
        conditionRepository = mock(CollectionConditionRepository.class);
        sourceRepository = mock(CollectionSourceRepository.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);

        service = new CollectionConditionService(
                conditionRepository,
                sourceRepository,
                companyIdProvider
        );
    }

    @Test
    @DisplayName("현재 회사 ID로 삭제되지 않은 수집 조건만 조회한다")
    void getsConditionsByCurrentCompany() {
        when(conditionRepository.findAllNotDeleted(COMPANY_ID))
                .thenReturn(List.of());

        assertThat(service.getAll()).isEmpty();

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
                        USER_ID
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
                        USER_ID
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
                USER_ID
        );
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