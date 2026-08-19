package com.group3.vitamins.major.application;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.major.application.command.CreateMajorCommand;
import com.group3.vitamins.major.application.command.UpdateMajorCommand;
import com.group3.vitamins.major.application.port.MajorQueryPort;
import com.group3.vitamins.major.application.result.MajorResult;
import com.group3.vitamins.major.application.service.MajorCommandService;
import com.group3.vitamins.major.domain.exception.MajorErrorCode;
import com.group3.vitamins.major.domain.model.Major;
import com.group3.vitamins.major.domain.repository.MajorRepository;
import com.group3.vitamins.qualification.application.policy.QualificationAdminPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 전공 마스터 생성·수정의 <b>이름 규칙</b> 검증 (qualification.md 공통 원칙, 2026-08-18 금지 문자 추가).
 * 쉼표·세미콜론·콜론이 든 이름은 사원 엑셀 파서가 쪼개므로 저장 자체를 막아야 한다.
 */
@DisplayName("MajorCommandService 이름 규칙")
class MajorCommandServiceCreateTest {

    private MajorRepository majorRepository;
    private MajorCommandService service;

    @BeforeEach
    void setUp() {
        majorRepository = Mockito.mock(MajorRepository.class);
        MajorQueryPort majorQueryPort = Mockito.mock(MajorQueryPort.class);
        CurrentCompanyIdProvider companyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(companyIdProvider.currentCompanyId()).thenReturn(1L);
        service = new MajorCommandService(new QualificationAdminPolicy(), companyIdProvider,
                majorRepository, majorQueryPort);

        when(majorRepository.findByName(anyString(), anyLong())).thenReturn(Optional.empty());
        when(majorRepository.save(any())).thenAnswer(inv -> {
            Major m = inv.getArgument(0);
            return Major.restore(99L, m.getCompanyId(), m.getName(), LocalDateTime.now());
        });
    }

    @Test
    @DisplayName("보통 이름은 공백을 걷어내고 저장한다")
    void createsWithStrippedName() {
        MajorResult r = service.create(new CreateMajorCommand("  컴퓨터공학 ", "ADMIN"));

        ArgumentCaptor<Major> cap = ArgumentCaptor.forClass(Major.class);
        verify(majorRepository).save(cap.capture());
        assertThat(cap.getValue().getName()).isEqualTo("컴퓨터공학");
        assertThat(r.majorId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("쉼표·세미콜론·콜론이 든 이름은 MAJOR_INVALID_REQUEST — 저장하지 않는다(엑셀 구분자 충돌)")
    void rejectsSeparatorChars() {
        for (String bad : new String[]{"컴퓨터공학, 산업공학", "A;B", "컴퓨터공학:학사"}) {
            assertThatThrownBy(() -> service.create(new CreateMajorCommand(bad, "ADMIN")))
                    .satisfies(hasCode(MajorErrorCode.MAJOR_INVALID_REQUEST));
        }
        verify(majorRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정도 같은 규칙 — 콜론이 든 새 이름은 MAJOR_INVALID_REQUEST")
    void updateRejectsSeparatorChars() {
        assertThatThrownBy(() -> service.update(new UpdateMajorCommand(10L, "전공:학사", "ADMIN")))
                .satisfies(hasCode(MajorErrorCode.MAJOR_INVALID_REQUEST));
        verify(majorRepository, never()).findById(anyLong(), anyLong()); // 이름 검사가 조회보다 먼저
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return t -> assertThat(t).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(expected);
    }
}
