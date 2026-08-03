package com.group3.vitamins.text.application.policy;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.text.application.port.BlockCatalogPort;
import com.group3.vitamins.text.domain.exception.TextErrorCode;
import com.group3.vitamins.text.domain.model.Text;
import com.group3.vitamins.text.domain.repository.TextRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TextEligibilityPolicy {

    private static final String BLOCK_TYPE = "TEXT";

    private final BlockCatalogPort blockCatalogPort;
    private final TextRepository textRepository;

    public Text getActiveTextOrThrow(Long txtId) {
        return textRepository.findActiveByTxtId(txtId)
                .orElseThrow(() -> {
                    log.warn("텍스트 블록 존재하지 않음 - txtId={}", txtId);
                    return new NotFoundException(TextErrorCode.BLOCK_NOT_FOUND);
                });
    }

    public void assertEditPermission(Long txtId, String userId) {
        if (!blockCatalogPort.hasEditPermission(BLOCK_TYPE, txtId, userId)) {
            log.warn("편집 권한 없음 - blockType={}, txtId={}, userId={}", BLOCK_TYPE, txtId, userId);
            throw new ForbiddenException(TextErrorCode.FORBIDDEN);
        }
    }
}
