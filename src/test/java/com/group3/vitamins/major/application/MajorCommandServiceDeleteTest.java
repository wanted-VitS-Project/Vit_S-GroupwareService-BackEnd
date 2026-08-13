package com.group3.vitamins.major.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.major.application.command.DeleteMajorCommand;
import com.group3.vitamins.major.application.port.MajorQueryPort;
import com.group3.vitamins.major.application.service.MajorCommandService;
import com.group3.vitamins.major.domain.exception.MajorErrorCode;
import com.group3.vitamins.major.domain.model.Major;
import com.group3.vitamins.major.domain.repository.MajorRepository;
import com.group3.vitamins.qualification.application.policy.QualificationAdminPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 전공 마스터 삭제 가드 검증 (CodeRabbit #361 반영). 핵심은 <b>퇴사·시스템 사원 참조도 삭제를 막아야</b> 한다는 것 —
 * 삭제 판정을 활성 수({@code employeeCount})가 아니라 전체 참조 수({@code countReferences})로 한다.
 */
@DisplayName("MajorCommandService 삭제 가드")
class MajorCommandServiceDeleteTest {

    private MajorRepository majorRepository;
    private MajorQueryPort majorQueryPort;
    private MajorCommandService service;

    @BeforeEach
    void setUp() {
        majorRepository = Mockito.mock(MajorRepository.class);
        majorQueryPort = Mockito.mock(MajorQueryPort.class);
        CurrentCompanyIdProvider companyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(companyIdProvider.currentCompanyId()).thenReturn(1L);
        service = new MajorCommandService(new QualificationAdminPolicy(), companyIdProvider,
                majorRepository, majorQueryPort);

        Major major = Mockito.mock(Major.class);
        when(major.getMajorId()).thenReturn(10L);
        when(majorRepository.findById(10L, 1L)).thenReturn(Optional.of(major));
    }

    private DeleteMajorCommand cmd() {
        return new DeleteMajorCommand(10L, "ADMIN");
    }

    @Test
    @DisplayName("전체 참조 수가 0보다 크면 MAJOR_IN_USE — 삭제하지 않는다(퇴사자만 참조해도 차단)")
    void blocksWhenReferenced() {
        when(majorQueryPort.countReferences(10L, 1L)).thenReturn(2L); // 활성 0이어도 전체 2면 차단

        assertThatThrownBy(() -> service.delete(cmd()))
                .satisfies(hasCode(MajorErrorCode.MAJOR_IN_USE))
                .hasMessageContaining("2명");
        verify(majorRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("참조가 없으면 삭제한다")
    void deletesWhenUnreferenced() {
        when(majorQueryPort.countReferences(10L, 1L)).thenReturn(0L);

        assertThatCode(() -> service.delete(cmd())).doesNotThrowAnyException();
        verify(majorRepository).deleteById(10L);
    }

    @Test
    @DisplayName("선검사~삭제 경합으로 FK 위반이 나면 MAJOR_IN_USE 로 변환한다(500 방지)")
    void convertsFkViolationOnRace() {
        when(majorQueryPort.countReferences(10L, 1L)).thenReturn(0L); // 선검사는 통과
        doThrow(new DataIntegrityViolationException("fk")).when(majorRepository).deleteById(10L);

        assertThatThrownBy(() -> service.delete(cmd()))
                .satisfies(hasCode(MajorErrorCode.MAJOR_IN_USE));
    }

    @Test
    @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 조회·삭제까지 가지 않는다")
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> service.delete(new DeleteMajorCommand(10L, "MASTER")))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
        verify(majorQueryPort, never()).countReferences(any(), any());
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return t -> assertThat(t).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(expected);
    }
}
