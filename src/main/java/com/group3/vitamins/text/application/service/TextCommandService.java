package com.group3.vitamins.text.application.service;

import com.group3.vitamins.text.application.command.UpdateTextContentCommand;
import com.group3.vitamins.text.application.policy.TextEligibilityPolicy;
import com.group3.vitamins.text.application.usecase.TextCommandUseCase;
import com.group3.vitamins.text.domain.model.Text;
import com.group3.vitamins.text.domain.repository.TextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 텍스트 블록 생성·삭제는 Block 도메인(동훈님)이 처리한다 — 여기는 본문 수정(PATCH)만 담당한다.
 * 블록 삭제 시 정리 로직은 {@link TextHandlerService} 가 이벤트 리스너를 통해 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TextCommandService implements TextCommandUseCase {

    private final TextEligibilityPolicy eligibilityPolicy;
    private final TextRepository textRepository;

    @Override
    public Text updateContent(UpdateTextContentCommand command) {
        log.info("텍스트 본문 수정 요청 - txtId={}, userId={}", command.txtId(), command.userId());

        eligibilityPolicy.getActiveTextOrThrow(command.txtId());
        eligibilityPolicy.assertEditPermission(command.txtId(), command.userId());

        Text saved = textRepository.updateContent(command.txtId(), command.content());

        log.info("텍스트 본문 수정 완료 - txtId={}", saved.getTxtId());

        // TODO: 활동 로그(본문 수정) 이벤트 발행 — 활동 로그 인프라(ActivityOccurredEvent 등)가 아직
        //       실제로 만들어지지 않아 주석으로만 남긴다. 실제로 값이 바뀐 경우에만 발행한다 (§5.4 텍스트 Block).
        // String beforeContent = text.getContent();
        // if (!Objects.equals(beforeContent, saved.getContent())) {
        //     List<ActivityFieldChange> changes = List.of(
        //             new ActivityFieldChange("content", beforeContent, saved.getContent())
        //     );
        //     activityEventPublisher.publish(
        //             ActivityOccurredEvent.modified(saved.getBlockId(), saved.getTxtId(), command.userId(), changes)
        //     );
        // }

        return saved;
    }
}
