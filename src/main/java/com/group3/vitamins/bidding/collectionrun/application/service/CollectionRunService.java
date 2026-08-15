package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectionrun.application.command.StartCollectionRunCommand;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunConditionPort;
import com.group3.vitamins.bidding.collectionrun.application.query.GetCollectionRunQuery;
import com.group3.vitamins.bidding.collectionrun.application.result.CollectionRunResult;
import com.group3.vitamins.bidding.collectionrun.application.support.CollectionRunCreator;
import com.group3.vitamins.bidding.collectionrun.application.usecase.CollectionRunUseCase;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
import com.group3.vitamins.bidding.collectionrun.domain.repository.CollectionRunRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class CollectionRunService implements CollectionRunUseCase {

    private static final int MAX_MANUAL_RANGE_DAYS = 31;

    private final CollectionRunConditionPort conditionPort;
    private final CollectionRunRepository runRepository;
    private final CollectionRunCreator runCreator;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final Clock clock;

    // 현재 회사의 활성 수집 조건으로 새로운 수집 실행을 생성합니다.
    @Override
    public CollectionRunResult start(StartCollectionRunCommand command) {
        validateStartCommand(command);
        validateCustomRange(command);

        Long companyId = currentCompanyIdProvider.currentCompanyId();

        CollectionCondition condition = conditionPort
                .findOwnedConditionForUpdate(
                        command.conditionId(),
                        companyId
                )
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_COLLECTION_CONDITION_NOT_FOUND
                ));

        validateConditionIsActive(condition);
        validateNoActiveRun(condition.getConditionId());

        CollectionRun savedRun = runCreator.create(
                condition,
                companyId,
                CollectionRunTriggerType.MANUAL,
                command.userId(),
                LocalDateTime.now(clock),
                command.startedAt(),
                command.endedAt()
        );

        return CollectionRunResult.from(savedRun);
    }

    // 현재 회사가 소유한 수집 실행 결과만 조회합니다.
    @Override
    @Transactional(readOnly = true)
    public CollectionRunResult get(GetCollectionRunQuery query) {
        validateGetQuery(query);

        Long companyId = currentCompanyIdProvider.currentCompanyId();

        CollectionRun run = runRepository
                .findByIdAndCompanyId(query.runId(), companyId)
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_COLLECTION_RUN_NOT_FOUND
                ));

        return CollectionRunResult.from(run);
    }

    // 수집 실행 생성 요청의 필수값을 검증합니다.
    private void validateStartCommand(StartCollectionRunCommand command) {
        if (command == null
                || command.conditionId() == null
                || command.conditionId() <= 0
                || command.userId() == null
                || command.userId().isBlank()) {
            throw invalidRunRequest();
        }
    }

    // 수집 실행 조회 요청의 실행 ID를 검증합니다.
    private void validateGetQuery(GetCollectionRunQuery query) {
        if (query == null
                || query.runId() == null
                || query.runId() <= 0) {
            throw invalidRunRequest();
        }
    }

    // 수동 지정 조회 구간이 있으면 함께 지정됐는지, 순서와 최대 폭을 지켰는지 검증합니다.
    private void validateCustomRange(StartCollectionRunCommand command) {
        LocalDateTime startedAt = command.startedAt();
        LocalDateTime endedAt = command.endedAt();

        if (startedAt == null && endedAt == null) {
            return;
        }
        if (startedAt == null || endedAt == null || !startedAt.isBefore(endedAt)) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_COLLECTION_RUN_RANGE_INVALID
            );
        }
        if (Duration.between(startedAt, endedAt).toDays() > MAX_MANUAL_RANGE_DAYS) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_COLLECTION_RUN_RANGE_TOO_WIDE
            );
        }
    }

    // 비활성화된 수집 조건의 실행을 차단합니다.
    private void validateConditionIsActive(CollectionCondition condition) {
        if (!condition.isActive()) {
            throw new ValidationException(
                    BiddingErrorCode.BIDDING_INACTIVE_COLLECTION_CONDITION
            );
        }
    }

    // 동일한 조건에 진행 중인 실행이 있으면 중복 실행을 차단합니다.
    private void validateNoActiveRun(Long conditionId) {
        if (runRepository.existsActiveByConditionId(conditionId)) {
            throw new ConflictException(
                    BiddingErrorCode.BIDDING_COLLECTION_RUN_ALREADY_PROCESSING
            );
        }
    }

    // 잘못된 수집 실행 요청 예외를 생성합니다.
    private ValidationException invalidRunRequest() {
        return new ValidationException(
                BiddingErrorCode.BIDDING_INVALID_COLLECTION_RUN_REQUEST
        );
    }
}
