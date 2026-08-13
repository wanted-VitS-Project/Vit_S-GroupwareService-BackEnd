package com.group3.vitamins.companydocument.domain.model;

import java.time.LocalDateTime;

/**
 * 사내 문서(논리 문서) 도메인 객체 (COMPANY-DOC-V1 · `company_document`).
 *
 * <p>순수 도메인이다 — JPA·Spring 에 의존하지 않는다.
 * ⭐ **사내 문서는 회사(테넌트) 소속**(`companyId`)이다 — 프로젝트·블록·스텝에 붙지 않는다(INV-01).
 * file 과 달리 낙관락이 없다(단순화, §6-4). 삭제는 soft delete + 복구만이다(휴지통·영구삭제 2단 미도입).
 *
 * <p>버전은 별도 엔티티({@link CompanyDocumentVersion})로 1:N 이며 여기서 직접 들고 있지 않는다.
 */
public class CompanyDocument {

    private final Long companyDocumentId;
    private final Long companyId;
    private DocumentCategory category;
    private String name;
    private final String createdBy;
    private LocalDateTime deletedAt;

    private CompanyDocument(Long companyDocumentId, Long companyId, DocumentCategory category,
                            String name, String createdBy, LocalDateTime deletedAt) {
        this.companyDocumentId = companyDocumentId;
        this.companyId = companyId;
        this.category = category;
        this.name = name;
        this.createdBy = createdBy;
        this.deletedAt = deletedAt;
    }

    /** 새 문서를 만든다(버전 1 업로드 시작 시). 아직 저장 전이라 ID 가 없다. */
    public static CompanyDocument create(Long companyId, DocumentCategory category, String name, String createdBy) {
        return new CompanyDocument(null, companyId, category, name, createdBy, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static CompanyDocument restore(Long companyDocumentId, Long companyId, DocumentCategory category,
                                          String name, String createdBy, LocalDateTime deletedAt) {
        return new CompanyDocument(companyDocumentId, companyId, category, name, createdBy, deletedAt);
    }

    /** 표시명을 바꾼다(§4). 원본 파일명은 버전에 있으므로 건드리지 않는다. */
    public void rename(String name) {
        this.name = name;
    }

    /** 카테고리를 바꾼다(§4). */
    public void changeCategory(DocumentCategory category) {
        this.category = category;
    }

    /** soft delete(§5). 저장소 객체는 지우지 않는다. 이미 삭제 상태인지 판정은 서비스가 한다. */
    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }

    /** 복구(§6). 삭제 시각을 지운다. 삭제 상태 여부 판정은 서비스가 한다. */
    public void restoreFromTrash() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Long getCompanyDocumentId() {
        return companyDocumentId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
