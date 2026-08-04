package com.group3.vitamins.businesscategory.application.service;

import com.group3.vitamins.businesscategory.application.command.CreateBusinessCategoryCommand;
import com.group3.vitamins.businesscategory.application.policy.BusinessCategoryAdminPolicy;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BusinessCategoryCommandService 생성")
class BusinessCategoryCommandServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);

    private BusinessCategoryRepository businessCategoryRepository;
    private BusinessCategoryCommandService businessCategoryCommandService;

    @BeforeEach
    void setUp() {
        businessCategoryRepository = Mockito.mock(BusinessCategoryRepository.class);
        businessCategoryCommandService = new BusinessCategoryCommandService(
                businessCategoryRepository,
                new BusinessCategoryAdminPolicy());
    }

    @Test
    @DisplayName("ADMIN 이 이름·업무코드·설명으로 생성하면 deletable 이 true 다")
    void createSucceeds() {
        when(businessCategoryRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(businessCategoryRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(businessCategoryRepository.save(any(BusinessCategory.class)))
                .thenReturn(BusinessCategory.restore(4L, "상하수도", "WATER", "설명", CREATED_AT, null));

        BusinessCategoryResult result = businessCategoryCommandService.createCategory(
                command("상하수도", "WATER", "설명", "ADMIN"));

        assertThat(result.categoryId()).isEqualTo(4L);
        assertThat(result.deletable()).isTrue();
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("code 가 null 이면 코드 중복 검사를 하지 않는다")
    void skipsCodeCheckWhenCodeIsNull() {
        when(businessCategoryRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(businessCategoryRepository.save(any(BusinessCategory.class)))
                .thenReturn(BusinessCategory.restore(1L, "환경", null, null, CREATED_AT, null));

        businessCategoryCommandService.createCategory(command("환경", null, null, "ADMIN"));

        verify(businessCategoryRepository, never()).findByCode(anyString());
    }

    @Test
    @DisplayName("MASTER 는 403 BUSINESS_CATEGORY_ADMIN_ONLY 다")
    void masterCannotCreate() {
        assertDomainException(
                () -> businessCategoryCommandService.createCategory(command("환경", null, null, "MASTER")),
                403, BusinessCategoryErrorCode.BUSINESS_CATEGORY_ADMIN_ONLY);

        verify(businessCategoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("이름이 없으면 400 BUSINESS_CATEGORY_NAME_REQUIRED 다")
    void blankNameRejected() {
        assertDomainException(
                () -> businessCategoryCommandService.createCategory(command(" ", null, null, "ADMIN")),
                400, BusinessCategoryErrorCode.BUSINESS_CATEGORY_NAME_REQUIRED);
    }

    @Test
    @DisplayName("이름이 100자를 넘으면 400 BUSINESS_CATEGORY_FIELD_TOO_LONG 이다")
    void nameTooLongRejected() {
        String tooLong = "가".repeat(101);

        assertDomainException(
                () -> businessCategoryCommandService.createCategory(command(tooLong, null, null, "ADMIN")),
                400, BusinessCategoryErrorCode.BUSINESS_CATEGORY_FIELD_TOO_LONG);
    }

    @Test
    @DisplayName("code 가 빈 문자열이면 400 BUSINESS_CATEGORY_CODE_INVALID 다")
    void emptyCodeRejected() {
        assertDomainException(
                () -> businessCategoryCommandService.createCategory(command("환경", "", null, "ADMIN")),
                400, BusinessCategoryErrorCode.BUSINESS_CATEGORY_CODE_INVALID);
    }

    @Test
    @DisplayName("code 가 30자를 넘으면 400 BUSINESS_CATEGORY_FIELD_TOO_LONG 이다")
    void codeTooLongRejected() {
        String tooLong = "A".repeat(31);

        assertDomainException(
                () -> businessCategoryCommandService.createCategory(command("환경", tooLong, null, "ADMIN")),
                400, BusinessCategoryErrorCode.BUSINESS_CATEGORY_FIELD_TOO_LONG);
    }

    @Test
    @DisplayName("살아있는 이름과 중복되면 409 이고 메시지는 enum 기본값이다")
    void duplicateActiveNameRejected() {
        BusinessCategory active = BusinessCategory.restore(1L, "환경", null, null, CREATED_AT, null);
        when(businessCategoryRepository.findByName("환경")).thenReturn(Optional.of(active));

        DomainException exception = assertDomainException(
                () -> businessCategoryCommandService.createCategory(command("환경", null, null, "ADMIN")),
                409, BusinessCategoryErrorCode.BUSINESS_CATEGORY_NAME_DUPLICATED);

        assertThat(exception.getMessage())
                .isEqualTo(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("삭제된 이름과 중복되면 409 이고 삭제분 안내 문구가 붙는다")
    void duplicateDeletedNameRejected() {
        BusinessCategory deleted = BusinessCategory.restore(
                1L, "환경", null, null, CREATED_AT, LocalDateTime.of(2026, 8, 1, 0, 0));
        when(businessCategoryRepository.findByName("환경")).thenReturn(Optional.of(deleted));

        DomainException exception = assertDomainException(
                () -> businessCategoryCommandService.createCategory(command("환경", null, null, "ADMIN")),
                409, BusinessCategoryErrorCode.BUSINESS_CATEGORY_NAME_DUPLICATED);

        assertThat(exception.getMessage()).isEqualTo("삭제된 카테고리에 같은 이름이 있습니다. 다른 이름을 쓰세요.");
    }

    @Test
    @DisplayName("살아있는 업무코드와 중복되면 409 이고 메시지는 enum 기본값이다")
    void duplicateActiveCodeRejected() {
        when(businessCategoryRepository.findByName(anyString())).thenReturn(Optional.empty());
        BusinessCategory active = BusinessCategory.restore(2L, "상수도", "WATER", null, CREATED_AT, null);
        when(businessCategoryRepository.findByCode("WATER")).thenReturn(Optional.of(active));

        assertDomainException(
                () -> businessCategoryCommandService.createCategory(command("하수도", "WATER", null, "ADMIN")),
                409, BusinessCategoryErrorCode.BUSINESS_CATEGORY_CODE_DUPLICATED);
    }

    @Test
    @DisplayName("삭제된 업무코드와 중복되면 409 이고 삭제분 안내 문구가 붙는다")
    void duplicateDeletedCodeRejected() {
        when(businessCategoryRepository.findByName(anyString())).thenReturn(Optional.empty());
        BusinessCategory deleted = BusinessCategory.restore(
                2L, "상수도", "WATER", null, CREATED_AT, LocalDateTime.of(2026, 8, 1, 0, 0));
        when(businessCategoryRepository.findByCode("WATER")).thenReturn(Optional.of(deleted));

        DomainException exception = assertDomainException(
                () -> businessCategoryCommandService.createCategory(command("하수도", "WATER", null, "ADMIN")),
                409, BusinessCategoryErrorCode.BUSINESS_CATEGORY_CODE_DUPLICATED);

        assertThat(exception.getMessage()).isEqualTo("삭제된 카테고리에 같은 업무코드가 있습니다. 다른 코드를 쓰세요.");
    }

    private DomainException assertDomainException(ThrowingCallable callable, int httpStatus,
                                                    BusinessCategoryErrorCode errorCode) {
        DomainException[] captured = new DomainException[1];
        assertThatThrownBy(callable::call)
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    DomainException exception = (DomainException) thrown;
                    assertThat(exception.getHttpStatus()).isEqualTo(httpStatus);
                    assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                    captured[0] = exception;
                });
        return captured[0];
    }

    private interface ThrowingCallable {
        void call();
    }

    private CreateBusinessCategoryCommand command(String name, String code, String description, String role) {
        return new CreateBusinessCategoryCommand(name, code, description, role);
    }
}
