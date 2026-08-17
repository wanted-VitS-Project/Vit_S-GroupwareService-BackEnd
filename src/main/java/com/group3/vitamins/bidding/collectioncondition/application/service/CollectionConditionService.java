package com.group3.vitamins.bidding.collectioncondition.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.command.CreateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.application.command.UpdateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.application.result.CollectionConditionResult;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.application.usecase.CollectionConditionUseCase;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionLookbackPeriod;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionSource;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionConditionRepository;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionSourceRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.Clock;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionScheduleType;

@Service
@Transactional
public class CollectionConditionService implements CollectionConditionUseCase {

    private static final int MAX_SOURCE_CODE_LENGTH = 30;
    private static final int MAX_CONDITION_NAME_LENGTH = 100;
    private static final int MAX_QUERY_COMBINATION_COUNT = 20;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_REGION_CODE_LENGTH = 20;
    private static final int MAX_INDUSTRY_CODE_LENGTH = 30;
    private static final String SUPPORTED_TIMEZONE = "Asia/Seoul";
    private static final ZoneId SEOUL_ZONE = ZoneId.of(SUPPORTED_TIMEZONE);
    private static final Set<LocalTime> SUPPORTED_SCHEDULE_TIMES = Set.of(
            LocalTime.of(9, 0), LocalTime.of(13, 0), LocalTime.of(18, 0)
    );

    private final CollectionConditionRepository conditionRepository;
    private final CollectionSourceRepository sourceRepository;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final BiddingAccessPolicy biddingAccessPolicy;
    private final Clock clock;

    @Autowired
    public CollectionConditionService(
            CollectionConditionRepository conditionRepository,
            CollectionSourceRepository sourceRepository,
            CurrentCompanyIdProvider currentCompanyIdProvider,
            BiddingAccessPolicy biddingAccessPolicy
    ) {
        this(conditionRepository, sourceRepository, currentCompanyIdProvider,
                biddingAccessPolicy, Clock.system(SEOUL_ZONE));
    }

    // 고정 Clock으로 시간대 경계 조건을 재현할 수 있게 테스트 진입점을 제공합니다.
    CollectionConditionService(
            CollectionConditionRepository conditionRepository,
            CollectionSourceRepository sourceRepository,
            CurrentCompanyIdProvider currentCompanyIdProvider,
            BiddingAccessPolicy biddingAccessPolicy,
            Clock clock
    ) {
        this.conditionRepository = conditionRepository;
        this.sourceRepository = sourceRepository;
        this.currentCompanyIdProvider = currentCompanyIdProvider;
        this.biddingAccessPolicy = biddingAccessPolicy;
        this.clock = clock;
    }

    // 현재 회사가 소유한 수집 조건 목록만 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public List<CollectionConditionResult> getAll(String userId, String role) {
        biddingAccessPolicy.assertAccess(userId, role);
        Long companyId = currentCompanyIdProvider.currentCompanyId();
        List<CollectionCondition> conditions =
                conditionRepository.findAllNotDeleted(companyId);

        Map<String, String> sourceNameCache = new HashMap<>();

        return conditions.stream()
                .map(condition -> CollectionConditionResult.from(
                        condition,
                        sourceNameCache.computeIfAbsent(
                                condition.getSourceCode(),
                                this::resolveSourceName
                        )
                ))
                .toList();
    }

    // 현재 회사 소속으로 새로운 수집 조건을 등록합니다.
    @Override
    public CollectionConditionResult create(
            CreateCollectionConditionCommand command
    ) {
        validateCreateCommand(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        CollectionSource source = getAvailableSource(command.sourceCode());

        LocalDateTime now = LocalDateTime.now(clock.withZone(SEOUL_ZONE));
        CollectionCondition condition = CollectionCondition.create(
                companyId,
                source.sourceCode(),
                command.conditionName().trim(),
                command.noticeTypes(),
                command.filters(),
                resolveLookbackPeriod(command.lookbackPeriod()),
                command.active(),
                command.autoCollectionEnabled(),
                command.scheduleType(),
                command.scheduledTime(),
                command.timezone(),
                calculateNextRunAt(command.active(), command.autoCollectionEnabled(),
                        command.scheduleType(), command.scheduledTime(), now),
                command.userId(),
                now
        );

        CollectionCondition saved = conditionRepository.save(condition);
        return CollectionConditionResult.from(saved, source.sourceName());
    }

    // 현재 회사가 소유한 수집 조건만 수정합니다.
    @Override
    public CollectionConditionResult update(
            UpdateCollectionConditionCommand command
    ) {
        validateUpdateCommand(command);
        biddingAccessPolicy.assertAccess(command.userId(), command.role());

        Long companyId = currentCompanyIdProvider.currentCompanyId();

        CollectionCondition condition = conditionRepository
                .findNotDeletedById(command.conditionId(), companyId)
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_COLLECTION_CONDITION_NOT_FOUND
                ));

        LocalDateTime now = LocalDateTime.now(clock.withZone(SEOUL_ZONE));
        condition.update(
                command.conditionName().trim(),
                command.noticeTypes(),
                command.filters(),
                resolveLookbackPeriod(command.lookbackPeriod()),
                command.active(),
                command.autoCollectionEnabled(),
                command.scheduleType(),
                command.scheduledTime(),
                command.timezone(),
                calculateNextRunAt(command.active(), command.autoCollectionEnabled(),
                        command.scheduleType(), command.scheduledTime(), now),
                now
        );

        CollectionCondition saved = conditionRepository.save(condition);
        String sourceName = resolveSourceName(saved.getSourceCode());

        return CollectionConditionResult.from(saved, sourceName);
    }

    // 등록 요청의 필수값과 수집 조건을 검증합니다.
    private void validateCreateCommand(
            CreateCollectionConditionCommand command
    ) {
        if (command == null
                || isBlank(command.sourceCode())
                || command.sourceCode().length() > MAX_SOURCE_CODE_LENGTH
                || command.active() == null
                || isBlank(command.userId())) {
            throw invalidCondition();
        }



        validateCommonCondition(
                command.conditionName(),
                command.noticeTypes(),
                command.filters()
        );
        validateSchedule(command.active(), command.autoCollectionEnabled(),
                command.scheduleType(), command.scheduledTime(), command.timezone());
    }

    // 수정 요청의 식별자와 수집 조건을 검증합니다.
    private void validateUpdateCommand(
            UpdateCollectionConditionCommand command
    ) {
        if (command == null
                || command.conditionId() == null
                || command.conditionId() <= 0
                || command.active() == null
                || isBlank(command.userId())) {
            throw invalidCondition();
        }

        validateCommonCondition(
                command.conditionName(),
                command.noticeTypes(),
                command.filters()
        );
        validateSchedule(command.active(), command.autoCollectionEnabled(),
                command.scheduleType(), command.scheduledTime(), command.timezone());
    }

    // 자동 수집 활성 여부와 주기·시각·시간대 조합을 계약대로 검증합니다.
    private void validateSchedule(
            Boolean active, Boolean autoEnabled,
            CollectionScheduleType scheduleType, LocalTime scheduledTime,
            String timezone
    ) {
        if (autoEnabled == null) {
            throw invalidSchedule();
        }
        boolean scheduleValuesMissing = scheduleType == null
                && scheduledTime == null && timezone == null;
        if (!autoEnabled) {
            if (!scheduleValuesMissing) {
                throw invalidSchedule();
            }
            return;
        }
        if (!Boolean.TRUE.equals(active)
                || scheduleType == null
                || !SUPPORTED_SCHEDULE_TIMES.contains(scheduledTime)
                || !SUPPORTED_TIMEZONE.equals(timezone)) {
            throw invalidSchedule();
        }
    }

    // 등록·수정 시점 이후의 첫 자동 실행 시각을 계산합니다.
    private LocalDateTime calculateNextRunAt(
            Boolean active, Boolean autoEnabled,
            CollectionScheduleType scheduleType, LocalTime scheduledTime,
            LocalDateTime now
    ) {
        if (!Boolean.TRUE.equals(active) || !Boolean.TRUE.equals(autoEnabled)) {
            return null;
        }
        LocalDateTime candidate = now.toLocalDate().atTime(scheduledTime);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        if (scheduleType == CollectionScheduleType.WEEKDAYS) {
            while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY
                    || candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                candidate = candidate.plusDays(1);
            }
        }
        return candidate;
    }

    // 조회 기간을 지정하지 않으면 1주를 기본값으로 적용합니다.
    private CollectionLookbackPeriod resolveLookbackPeriod(
            CollectionLookbackPeriod requested
    ) {
        return requested == null ? CollectionLookbackPeriod.ONE_WEEK : requested;
    }

    private ValidationException invalidSchedule() {
        return new ValidationException(
                BiddingErrorCode.BIDDING_INVALID_COLLECTION_SCHEDULE
        );
    }

    // 등록과 수정에서 공통으로 사용하는 조건값을 검증합니다.
    private void validateCommonCondition(
            String conditionName,
            List<?> noticeTypes,
            CollectionConditionFilter filters
    ) {
        if (isBlank(conditionName)
                || conditionName.length() > MAX_CONDITION_NAME_LENGTH
                || noticeTypes == null
                || noticeTypes.isEmpty()
                || filters == null
                || filters.keywords() == null
                || filters.keywords().isEmpty()) {
            throw invalidCondition();
        }

        validateFilterStrings(filters);
        validateEstimatedPriceRange(filters);
        validateQueryCombinationCount(noticeTypes.size(), filters);
    }

    // 최소·최대 추정가격의 범위를 검증합니다.
    private void validateEstimatedPriceRange(
            CollectionConditionFilter filters
    ) {
        Long minimum = filters.minimumEstimatedPrice();
        Long maximum = filters.maximumEstimatedPrice();

        if ((minimum != null && minimum < 0)
                || (maximum != null && maximum < 0)
                || (minimum != null
                && maximum != null
                && minimum > maximum)) {
            throw invalidCondition();
        }
    }

    // 나라장터 API 호출 조합이 최대 20개를 넘지 않는지 확인합니다.
    private void validateQueryCombinationCount(
            int noticeTypeCount,
            CollectionConditionFilter filters
    ) {
        // 키워드는 필수지만 검증 순서가 바뀌어도 조합 수가 0이 되지 않게 방어합니다.
        int keywordCount = atLeastOne(filters.keywords().size());
        int regionCount = atLeastOne(filters.regionCodes().size());
        int industryCount = atLeastOne(filters.industryCodes().size());

        long combinationCount = (long) noticeTypeCount
                * keywordCount
                * regionCount
                * industryCount;

        if (combinationCount > MAX_QUERY_COMBINATION_COUNT) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_COLLECTION_QUERY_LIMIT_EXCEEDED
            );
        }
    }

    // 외부 수집 요청에 사용되는 문자열이 공백이 아니고 허용 길이 안인지 검증합니다.
    private void validateFilterStrings(CollectionConditionFilter filters) {
        if (containsInvalidValue(filters.keywords(), MAX_KEYWORD_LENGTH)
                || containsInvalidValue(filters.regionCodes(), MAX_REGION_CODE_LENGTH)
                || containsInvalidValue(filters.industryCodes(), MAX_INDUSTRY_CODE_LENGTH)) {
            throw invalidCondition();
        }
    }

    // 목록에 null·공백·길이 초과 값이 하나라도 있는지 확인합니다.
    private boolean containsInvalidValue(List<String> values, int maxLength) {
        return values == null || values.stream()
                .anyMatch(value -> isBlank(value) || value.length() > maxLength);
    }

    // 등록에 사용할 수 있는 활성 수집처를 반환합니다.
    private CollectionSource getAvailableSource(String sourceCode) {
        CollectionSource source = sourceRepository
                .findNotDeletedByCode(sourceCode.trim())
                .orElseThrow(() -> new ValidationException(
                        BiddingErrorCode.BIDDING_UNSUPPORTED_SOURCE
                ));

        if (!source.isAvailable()) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_UNSUPPORTED_SOURCE
            );
        }

        return source;
    }

    // 목록과 수정 응답에 사용할 수집처 표시명을 조회합니다.
    private String resolveSourceName(String sourceCode) {
        return sourceRepository.findNotDeletedByCode(sourceCode)
                .map(CollectionSource::sourceName)
                .orElse(sourceCode);
    }

    // 선택 조건이 없으면 외부 API 호출 기준에서는 전체 조건 한 개로 계산합니다.
    private int atLeastOne(int count) {
        return Math.max(count, 1);
    }

    // null 또는 공백 문자열인지 확인합니다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 잘못된 수집 조건 예외를 생성합니다.
    private ValidationException invalidCondition() {
        return new ValidationException(
                BiddingErrorCode.BIDDING_INVALID_COLLECTION_CONDITION
        );
    }
}
