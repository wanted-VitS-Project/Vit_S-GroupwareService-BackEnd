package com.group3.vitamins.bidding.collectioncondition.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.command.CreateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.application.command.UpdateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.application.result.CollectionConditionResult;
import com.group3.vitamins.bidding.collectioncondition.application.usecase.CollectionConditionUseCase;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionSource;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionConditionRepository;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionSourceRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class CollectionConditionService implements CollectionConditionUseCase {

    private static final int MAX_SOURCE_CODE_LENGTH = 30;
    private static final int MAX_CONDITION_NAME_LENGTH = 100;
    private static final int MAX_QUERY_COMBINATION_COUNT = 20;

    private final CollectionConditionRepository conditionRepository;
    private final CollectionSourceRepository sourceRepository;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    // 현재 회사가 소유한 수집 조건 목록만 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public List<CollectionConditionResult> getAll() {
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

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        CollectionSource source = getAvailableSource(command.sourceCode());

        CollectionCondition condition = CollectionCondition.create(
                companyId,
                source.sourceCode(),
                command.conditionName().trim(),
                command.noticeTypes(),
                command.filters(),
                command.active(),
                command.userId(),
                LocalDateTime.now()
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

        Long companyId = currentCompanyIdProvider.currentCompanyId();

        CollectionCondition condition = conditionRepository
                .findNotDeletedById(command.conditionId(), companyId)
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_COLLECTION_CONDITION_NOT_FOUND
                ));

        condition.update(
                command.conditionName().trim(),
                command.noticeTypes(),
                command.filters(),
                command.active(),
                LocalDateTime.now()
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
                || filters.keywords().isEmpty()) {
            throw invalidCondition();
        }

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
        int keywordCount = filters.keywords().size();
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