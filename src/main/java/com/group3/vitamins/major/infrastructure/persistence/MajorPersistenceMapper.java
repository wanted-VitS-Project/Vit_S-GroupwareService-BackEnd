package com.group3.vitamins.major.infrastructure.persistence;

import com.group3.vitamins.major.domain.model.Major;

/** {@link Major} 도메인 ↔ {@link MajorJpaEntity} 변환. createdAt 은 DB 관리라 복원 시 null(응답에 안 쓰임). */
public final class MajorPersistenceMapper {

    private MajorPersistenceMapper() {
    }

    public static Major toDomain(MajorJpaEntity e) {
        return Major.restore(e.getMajorId(), e.getCompanyId(), e.getName(), null);
    }

    public static MajorJpaEntity toEntity(Major d) {
        return new MajorJpaEntity(d.getMajorId(), d.getCompanyId(), d.getName());
    }
}
