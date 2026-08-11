package com.group3.vitamins.businesscategory.domain.model;

import java.time.LocalDateTime;

public class BusinessCategory {

    private final Long businessCategoryId;
    private final Long companyId;
    private String name;
    private String code;
    private String description;
    private final LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private BusinessCategory(Long businessCategoryId, Long companyId, String name, String code,
                            String description, LocalDateTime createdAt, LocalDateTime deletedAt) {
        this.businessCategoryId = businessCategoryId;
        this.companyId = companyId;
        this.name = name;
        this.code = code;
        this.description = description;
        this.createdAt = createdAt;
        this.deletedAt = deletedAt;
    }

    /** 새 카테고리를 만든다. 아직 저장되지 않았으므로 ID 가 없다. */
    public static BusinessCategory create(String name, String code, String description,
                                          LocalDateTime now, Long companyId) {
        return new BusinessCategory(null, companyId, name, code, description, now, null);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static BusinessCategory restore(Long businessCategoryId, Long companyId, String name,
                                           String code, String description, LocalDateTime createdAt,
                                           LocalDateTime deletedAt) {
        return new BusinessCategory(businessCategoryId, companyId, name, code, description,
                createdAt, deletedAt);
    }

    /** 이름을 바꾼다. */
    public void rename(String name) {
        this.name = name;
    }

    /** 업무코드를 바꾼다. null 을 넘기면 업무코드를 지운다. */
    public void changeCode(String code) {
        this.code = code;
    }

    /** 설명을 바꾼다. */
    public void changeDescription(String description) {
        this.description = description;
    }

    /** 논리 삭제 여부. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 논리 삭제한다. */
    public void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getBusinessCategoryId() { return businessCategoryId; }
    public Long getCompanyId() { return companyId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
}