package com.group3.vitamins.businesscategory.infrastructure.persistence;

import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * businessCategoryId 와 companyId 는 둘 다 {@code Long} 이라 위치 기반 생성자에서 뒤바뀌어도
 * 컴파일이 통과한다. 서로 다른 값을 넣어 왕복시켜 순서 어긋남을 잡는다.
 */
class BusinessCategoryMapperTest {

    private static final Long CATEGORY_ID = 7L;
    private static final Long COMPANY_ID = 9L;

    @Test
    @DisplayName("도메인 → 엔티티 → 도메인 왕복에서 두 식별자가 자기 자리를 지킨다")
    void 왕복_식별자_순서() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        BusinessCategory domain = BusinessCategory.restore(
                CATEGORY_ID, COMPANY_ID, "IT 용역", "IT_SERVICE", "설명", createdAt, null);

        BusinessCategoryJpaEntity entity = BusinessCategoryMapper.toEntity(domain);

        assertThat(entity.getBusinessCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(entity.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(entity.getName()).isEqualTo("IT 용역");
        assertThat(entity.getCode()).isEqualTo("IT_SERVICE");

        BusinessCategory restored = BusinessCategoryMapper.toDomain(entity);

        assertThat(restored.getBusinessCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(restored.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(restored.getName()).isEqualTo("IT 용역");
        assertThat(restored.getCode()).isEqualTo("IT_SERVICE");
    }

    @Test
    @DisplayName("생성 시 회사가 찍히고 ID 는 비어 있다")
    void 생성_스탬핑() {
        BusinessCategory created = BusinessCategory.create(
                "환경", "ENV", null, LocalDateTime.of(2026, 8, 10, 9, 0), COMPANY_ID);

        assertThat(created.getBusinessCategoryId()).isNull();
        assertThat(created.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(created.getName()).isEqualTo("환경");
        assertThat(created.isDeleted()).isFalse();
    }
}
