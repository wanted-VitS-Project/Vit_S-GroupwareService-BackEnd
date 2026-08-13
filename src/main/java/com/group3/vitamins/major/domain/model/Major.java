package com.group3.vitamins.major.domain.model;

import java.time.LocalDateTime;

/**
 * 전공 마스터 도메인 객체 (HR-V1 §2-G · `major`).
 *
 * <p>순수 도메인이다 — JPA·Spring 비의존. business_category 마스터를 미러링하되 code·description·soft delete 가 없다.
 * 삭제는 hard delete + 참조 차단(INV-18)이라 {@code deletedAt} 을 두지 않는다. 회사(테넌트) 소속.
 */
public class Major {

    private final Long majorId;
    private final Long companyId;
    private String name;
    private final LocalDateTime createdAt;

    private Major(Long majorId, Long companyId, String name, LocalDateTime createdAt) {
        this.majorId = majorId;
        this.companyId = companyId;
        this.name = name;
        this.createdAt = createdAt;
    }

    /** 새 전공을 만든다. 아직 저장 전이라 ID 가 없다. */
    public static Major create(String name, LocalDateTime now, Long companyId) {
        return new Major(null, companyId, name, now);
    }

    /** 저장된 데이터를 도메인 객체로 복원한다. */
    public static Major restore(Long majorId, Long companyId, String name, LocalDateTime createdAt) {
        return new Major(majorId, companyId, name, createdAt);
    }

    /** 이름을 바꾼다. */
    public void rename(String name) {
        this.name = name;
    }

    public Long getMajorId() {
        return majorId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
