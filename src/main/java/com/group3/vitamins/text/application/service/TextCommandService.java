package com.group3.vitamins.text.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.text.application.command.UpdateTextContentCommand;
import com.group3.vitamins.text.application.policy.TextEligibilityPolicy;
import com.group3.vitamins.text.application.usecase.TextCommandUseCase;
import com.group3.vitamins.text.domain.exception.TextErrorCode;
import com.group3.vitamins.text.domain.model.Text;
import com.group3.vitamins.text.domain.repository.TextRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 텍스트 블록 생성·삭제는 Block 도메인(동훈님)이 처리한다 — 여기는 본문 수정(PATCH)만 담당한다.
 * 블록 삭제 시 정리 로직은 {@link TextHandlerService} 가 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TextCommandService implements TextCommandUseCase {

    private final TextEligibilityPolicy eligibilityPolicy;
    private final TextRepository textRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public UpdateTextContentView updateContent(UpdateTextContentCommand command) {
        log.info("텍스트 본문 수정 요청 - txtId={}, userId={}", command.txtId(), command.userId());

        if (command.content() == null) {
            throw new ValidationException(TextErrorCode.INVALID_CONTENT);
        }

        Text before = eligibilityPolicy.getActiveTextOrThrow(command.txtId());
        eligibilityPolicy.assertEditPermission(command.txtId(), command.userId(), command.role());

        // 진입 시 검사는 편의(헛수고 방지)일 뿐 방어가 아니다 — 본체는 아래 조건부 UPDATE의
        // WHERE version = ? 다(CONCURRENCY.md §1-5). overwrite면 방금 읽은 DB 현재 버전을
        // 기대값으로 써서 반드시 통과시킨다.
        int expectedVersion = command.overwrite() ? before.getVersion() : command.version();
        LocalDateTime updatedAt = LocalDateTime.now();
        int updated = textRepository.updateContentIfVersionMatches(
                command.txtId(), command.content(), updatedAt, expectedVersion);
        if (updated == 0) {
            throw new ConflictException(TextErrorCode.TEXT_VERSION_CONFLICT);
        }

        log.info("텍스트 본문 수정 완료 - txtId={}", command.txtId());

        // 활동 로그(본문 수정) — 실제로 값이 바뀐 경우에만 발행한다 (§5.4 텍스트 Block).
        // 텍스트 본문은 표시명으로 쓰지 않으므로 resourceName은 null로 둔다.
        if (!Objects.equals(before.getContent(), command.content())) {
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.MODIFY,
                    before.getBlockId(),
                    command.txtId(),
                    null,
                    command.userId(),
                    List.of(new ActivityFieldChange("content", before.getContent(), command.content()))
            ));
        }

        return new UpdateTextContentView(command.txtId(), command.content(), updatedAt, expectedVersion + 1);
    }
}
