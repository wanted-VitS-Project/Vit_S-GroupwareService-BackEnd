package com.group3.vitamins.businesscategory.application.service;

import com.group3.vitamins.businesscategory.application.policy.BusinessCategoryAdminPolicy;
import com.group3.vitamins.businesscategory.application.port.ProjectCategoryLinkPort;
import com.group3.vitamins.businesscategory.application.query.BusinessCategoryListQuery;
import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;
import com.group3.vitamins.businesscategory.domain.exception.BusinessCategoryErrorCode;
import com.group3.vitamins.businesscategory.domain.model.BusinessCategory;
import com.group3.vitamins.businesscategory.domain.repository.BusinessCategoryRepository;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BusinessCategoryQueryService 목록 조회")
class BusinessCategoryQueryServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);

    private BusinessCategoryRepository businessCategoryRepository;
    private ProjectCategoryLinkPort projectCategoryLinkPort;
    private BusinessCategoryQueryService businessCategoryQueryService;

    @BeforeEach
    void setUp() {
        businessCategoryRepository = Mockito.mock(BusinessCategoryRepository.class);
        projectCategoryLinkPort = Mockito.mock(ProjectCategoryLinkPort.class);
        businessCategoryQueryService = new BusinessCategoryQueryService(
                businessCategoryRepository,
                projectCategoryLinkPort,
                new BusinessCategoryAdminPolicy());
    }

    @Test
    @DisplayName("연결된 프로젝트가 없으면 deletable 이 true 다")
    void deletableTrueWhenNotLinked() {
        when(businessCategoryRepository.search(isNull(), anyBoolean()))
                .thenReturn(List.of(category(1L, "도로 설계", "ROAD")));
        when(projectCategoryLinkPort.findLinkedCategoryIds()).thenReturn(Set.of());

        List<BusinessCategoryResult> results = businessCategoryQueryService.listCategories(query(null, false, "MEMBER"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).categoryId()).isEqualTo(1L);
        assertThat(results.get(0).deletable()).isTrue();
    }

    @Test
    @DisplayName("연결된 프로젝트가 있으면 deletable 이 false 다")
    void deletableFalseWhenLinked() {
        when(businessCategoryRepository.search(isNull(), anyBoolean()))
                .thenReturn(List.of(category(1L, "도로 설계", "ROAD"), category(2L, "수자원 관리", "WATER")));
        when(projectCategoryLinkPort.findLinkedCategoryIds()).thenReturn(Set.of(1L));

        List<BusinessCategoryResult> results = businessCategoryQueryService.listCategories(query(null, false, "MEMBER"));

        assertThat(results.get(0).deletable()).isFalse();
        assertThat(results.get(1).deletable()).isTrue();
    }

    @Test
    @DisplayName("0건이면 빈 목록을 내리고 프로젝트 도메인을 조회하지 않는다")
    void emptyListSkipsPort() {
        when(businessCategoryRepository.search(isNull(), anyBoolean())).thenReturn(List.of());

        assertThat(businessCategoryQueryService.listCategories(query(null, false, "MEMBER"))).isEmpty();

        verify(projectCategoryLinkPort, never()).findLinkedCategoryIds();
    }

    @Test
    @DisplayName("공백 keyword 는 검색 안 함으로 눕는다")
    void blankKeywordBecomesNull() {
        when(businessCategoryRepository.search(isNull(), anyBoolean())).thenReturn(List.of());

        businessCategoryQueryService.listCategories(query("   ", false, "MEMBER"));

        verify(businessCategoryRepository).search(null, false);
    }

    @Test
    @DisplayName("keyword 는 앞뒤 공백을 잘라 넘긴다")
    void keywordTrimmed() {
        when(businessCategoryRepository.search(anyString(), anyBoolean())).thenReturn(List.of());

        businessCategoryQueryService.listCategories(query("  수자원  ", false, "MEMBER"));

        verify(businessCategoryRepository).search("수자원", false);
    }

    @Test
    @DisplayName("ADMIN 은 includeDeleted=true 로 삭제분을 볼 수 있다")
    void adminCanIncludeDeleted() {
        BusinessCategory deleted = BusinessCategory.restore(
                3L, "스마트인프라", null, null, CREATED_AT, LocalDateTime.of(2026, 8, 1, 10, 0));
        when(businessCategoryRepository.search(isNull(), anyBoolean())).thenReturn(List.of(deleted));
        when(projectCategoryLinkPort.findLinkedCategoryIds()).thenReturn(Set.of());

        List<BusinessCategoryResult> results = businessCategoryQueryService.listCategories(query(null, true, "ADMIN"));

        assertThat(results.get(0).deletedAt()).isNotNull();
        verify(businessCategoryRepository).search(null, true);
    }

    @Test
    @DisplayName("MASTER 가 includeDeleted=true 를 요청하면 403 BUSINESS_CATEGORY_ADMIN_ONLY 다")
    void masterCannotIncludeDeleted() {
        assertThatThrownBy(() -> businessCategoryQueryService.listCategories(query(null, true, "MASTER")))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    DomainException exception = (DomainException) thrown;
                    assertThat(exception.getHttpStatus()).isEqualTo(403);
                    assertThat(exception.getErrorCode())
                            .isEqualTo(BusinessCategoryErrorCode.BUSINESS_CATEGORY_ADMIN_ONLY);
                });

        verify(businessCategoryRepository, never()).search(isNull(), anyBoolean());
    }

    @Test
    @DisplayName("MEMBER 가 includeDeleted=true 를 요청하면 403 이다")
    void memberCannotIncludeDeleted() {
        assertThatThrownBy(() -> businessCategoryQueryService.listCategories(query(null, true, "MEMBER")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("includeDeleted=false 면 MEMBER 도 조회할 수 있다")
    void memberCanListActiveOnly() {
        when(businessCategoryRepository.search(isNull(), anyBoolean()))
                .thenReturn(List.of(category(1L, "도시계획", "URBAN")));
        when(projectCategoryLinkPort.findLinkedCategoryIds()).thenReturn(Set.of());

        assertThat(businessCategoryQueryService.listCategories(query(null, false, "MEMBER"))).hasSize(1);
    }

    private BusinessCategoryListQuery query(String keyword, boolean includeDeleted, String role) {
        return new BusinessCategoryListQuery(keyword, includeDeleted, role);
    }

    private BusinessCategory category(Long id, String name, String code) {
        return BusinessCategory.restore(id, name, code, null, CREATED_AT, null);
    }
}