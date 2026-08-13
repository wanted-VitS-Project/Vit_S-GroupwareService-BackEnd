package com.group3.vitamins.companydocument.application.service;

import com.group3.vitamins.companydocument.application.command.DeleteCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.command.RestoreCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.command.UpdateCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.policy.CompanyDocumentAdminPolicy;
import com.group3.vitamins.companydocument.application.port.CompanyDocumentIndexTriggerPort;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentDeleteResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentRestoreResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentUpdateResult;
import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentCommandUseCase;
import com.group3.vitamins.companydocument.domain.exception.CompanyDocumentErrorCode;
import com.group3.vitamins.companydocument.domain.model.CompanyDocument;
import com.group3.vitamins.companydocument.domain.model.DocumentCategory;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 사내 문서 수정·삭제·복구 서비스 (§4·§5·§6). 전부 ADMIN 전용, 회사 스코프 강제.
 *
 * <p>낙관락이 없어(단순화, §6-4) 수정은 최종 저장이다. 삭제는 soft delete + 인덱스 제외 트리거, 복구는 인덱스 재등록 트리거.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyDocumentCommandService implements CompanyDocumentCommandUseCase {

    private static final int MAX_NAME_LENGTH = 255;

    private final CompanyDocumentAdminPolicy adminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final CompanyDocumentRepository documentRepository;
    private final CompanyDocumentIndexTriggerPort indexTriggerPort;

    @Override
    public CompanyDocumentUpdateResult update(UpdateCompanyDocumentCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();

        // ⚠️ 미전송(null)만 "변경 없음"이다. 전송된 공백 name 은 무시하지 않고 400 으로 막는다(조용한 무시 방지).
        //    category 는 원본 값으로 enum 검증한다 — " FINANCE " 처럼 공백이 낀 값을 strip 으로 통과시키지 않는다.
        String name = command.name();
        String category = command.category();
        if (name == null && category == null) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_INVALID_REQUEST);
        }
        if (name != null) {
            name = name.strip();
            if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
                throw new ValidationException(CompanyDocumentErrorCode.CDOC_INVALID_REQUEST);
            }
        }
        if (category != null && !DocumentCategory.isValid(category)) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_INVALID_REQUEST);
        }

        CompanyDocument document = requireOwnedDocument(command.companyDocumentId(), companyId);
        if (name != null) {
            document.rename(name);
        }
        if (category != null) {
            document.changeCategory(DocumentCategory.valueOf(category));
        }
        CompanyDocument saved = documentRepository.save(document);

        return new CompanyDocumentUpdateResult(
                saved.getCompanyDocumentId(), saved.getName(), saved.getCategory().name());
    }

    @Override
    public CompanyDocumentDeleteResult delete(DeleteCompanyDocumentCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();

        CompanyDocument document = findOwnedDocumentAllowDeleted(command.companyDocumentId(), companyId);
        if (document.isDeleted()) {
            // 400 — 명세·file(FILE_ALREADY_DELETED) 과 동일. Conflict(409)로 내면 계약 이탈이다.
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_ALREADY_DELETED);
        }

        document.delete(LocalDateTime.now());
        CompanyDocument saved = documentRepository.save(document);
        indexTriggerPort.triggerRemoval(saved.getCompanyDocumentId());

        return new CompanyDocumentDeleteResult(saved.getCompanyDocumentId(), saved.getDeletedAt());
    }

    @Override
    public CompanyDocumentRestoreResult restore(RestoreCompanyDocumentCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();

        CompanyDocument document = findOwnedDocumentAllowDeleted(command.companyDocumentId(), companyId);
        if (!document.isDeleted()) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_NOT_DELETED);
        }

        document.restoreFromTrash();
        CompanyDocument saved = documentRepository.save(document);
        indexTriggerPort.triggerReindex(saved.getCompanyDocumentId());

        return new CompanyDocumentRestoreResult(
                saved.getCompanyDocumentId(), saved.getName(), saved.getCategory().name());
    }

    /** 현재 회사 소속이며 삭제되지 않은 문서(수정 대상). 아니면 CDOC_NOT_FOUND. */
    private CompanyDocument requireOwnedDocument(Long companyDocumentId, long companyId) {
        return documentRepository.findById(companyDocumentId)
                .filter(d -> d.getCompanyId() == companyId && !d.isDeleted())
                .orElseThrow(() -> new NotFoundException(CompanyDocumentErrorCode.CDOC_NOT_FOUND));
    }

    /** 현재 회사 소속 문서(삭제 여부 무관 — 삭제/복구 상태 판정은 호출부가 한다). 아니면 CDOC_NOT_FOUND. */
    private CompanyDocument findOwnedDocumentAllowDeleted(Long companyDocumentId, long companyId) {
        return documentRepository.findById(companyDocumentId)
                .filter(d -> d.getCompanyId() == companyId)
                .orElseThrow(() -> new NotFoundException(CompanyDocumentErrorCode.CDOC_NOT_FOUND));
    }
}
