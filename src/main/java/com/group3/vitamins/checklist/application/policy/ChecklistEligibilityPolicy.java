package com.group3.vitamins.checklist.application.policy;

import com.group3.vitamins.checklist.domain.exception.ChecklistErrorCode;
import com.group3.vitamins.checklist.domain.model.ChecklistItem;
import com.group3.vitamins.checklist.domain.repository.ChecklistBlockRepository;
import com.group3.vitamins.checklist.domain.repository.ChecklistRepository;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.text.application.port.BlockCatalogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChecklistEligibilityPolicy {

    private static final String BLOCK_TYPE = "CHECKLIST";

    private final BlockCatalogPort blockCatalogPort;
    private final ChecklistRepository checklistRepository;
    private final ChecklistBlockRepository checklistBlockRepository;

    public void assertBlockActiveOrThrow(Long chkBlockId) {
        if (!checklistBlockRepository.existsActive(chkBlockId)) {
            log.warn("체크리스트 블록 존재하지 않음 - chkBlockId={}", chkBlockId);
            throw new NotFoundException(ChecklistErrorCode.BLOCK_NOT_FOUND);
        }
    }

    public ChecklistItem getActiveItemOrThrow(Long chkId) {
        return checklistRepository.findActiveByChkId(chkId)
                .orElseThrow(() -> {
                    log.warn("체크리스트 항목 존재하지 않음 - chkId={}", chkId);
                    return new NotFoundException(ChecklistErrorCode.ITEM_NOT_FOUND);
                });
    }

    public void assertEditPermission(Long chkBlockId, String userId, String role) {
        if (!blockCatalogPort.hasEditPermission(BLOCK_TYPE, chkBlockId, userId, role)) {
            log.warn("편집 권한 없음 - blockType={}, chkBlockId={}, userId={}", BLOCK_TYPE, chkBlockId, userId);
            throw new ForbiddenException(ChecklistErrorCode.FORBIDDEN);
        }
    }
}
