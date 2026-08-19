package com.group3.vitamins.employee.infrastructure;

import com.group3.vitamins.certificate.application.command.CreateCertificateCommand;
import com.group3.vitamins.certificate.application.result.CertificateResult;
import com.group3.vitamins.certificate.application.usecase.CertificateCommandUseCase;
import com.group3.vitamins.certificate.domain.exception.CertificateErrorCode;
import com.group3.vitamins.certificate.domain.model.Certificate;
import com.group3.vitamins.certificate.domain.repository.CertificateRepository;
import com.group3.vitamins.employee.infrastructure.adapter.QualificationMasterCreateAdapter;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.major.application.command.CreateMajorCommand;
import com.group3.vitamins.major.application.result.MajorResult;
import com.group3.vitamins.major.application.usecase.MajorCommandUseCase;
import com.group3.vitamins.major.domain.exception.MajorErrorCode;
import com.group3.vitamins.major.domain.model.Major;
import com.group3.vitamins.major.domain.repository.MajorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 엑셀 자동 생성 어댑터 — 상대 유스케이스로 만들고, 동명 충돌({@code *_NAME_DUPLICATED})이면 새로 만들지 않고 이름으로 재조회한 id 를 쓴다.
 */
@DisplayName("QualificationMasterCreateAdapter 마스터 자동 생성")
class QualificationMasterCreateAdapterTest {

    private MajorCommandUseCase majorCommandUseCase;
    private MajorRepository majorRepository;
    private CertificateCommandUseCase certificateCommandUseCase;
    private CertificateRepository certificateRepository;
    private QualificationMasterCreateAdapter adapter;

    @BeforeEach
    void setUp() {
        majorCommandUseCase = Mockito.mock(MajorCommandUseCase.class);
        majorRepository = Mockito.mock(MajorRepository.class);
        certificateCommandUseCase = Mockito.mock(CertificateCommandUseCase.class);
        certificateRepository = Mockito.mock(CertificateRepository.class);
        adapter = new QualificationMasterCreateAdapter(majorCommandUseCase, majorRepository,
                certificateCommandUseCase, certificateRepository);
    }

    @Test
    @DisplayName("전공 — 유스케이스로 이름마다 만들고 name→id 를 요청 순서대로 돌려준다")
    void createsMajorsInOrder() {
        when(majorCommandUseCase.create(new CreateMajorCommand("산업공학", "ADMIN"))).thenReturn(new MajorResult(40L, "산업공학"));
        when(majorCommandUseCase.create(new CreateMajorCommand("데이터과학", "ADMIN"))).thenReturn(new MajorResult(41L, "데이터과학"));

        Map<String, Long> ids = adapter.createMajors(List.of("산업공학", "데이터과학"), 1L, "ADMIN");

        assertThat(ids).containsExactly(Map.entry("산업공학", 40L), Map.entry("데이터과학", 41L));
        verify(majorRepository, never()).findByName(any(), any());
    }

    @Test
    @DisplayName("전공 — 동명 충돌이면 새로 만들지 않고 이름으로 재조회한 기존 id 를 쓴다(경합·대소문자 무시 collation)")
    void majorConflictFallsBackToLookup() {
        when(majorCommandUseCase.create(any())).thenThrow(new ConflictException(MajorErrorCode.MAJOR_NAME_DUPLICATED));
        when(majorRepository.findByName("sqld", 1L))
                .thenReturn(Optional.of(Major.restore(7L, 1L, "SQLD", LocalDateTime.now())));

        Map<String, Long> ids = adapter.createMajors(List.of("sqld"), 1L, "ADMIN");

        assertThat(ids).containsEntry("sqld", 7L);
    }

    @Test
    @DisplayName("전공 — 충돌인데 재조회도 실패하면 IllegalStateException(조용히 null 을 넣지 않는다)")
    void majorConflictWithoutMatchFails() {
        when(majorCommandUseCase.create(any())).thenThrow(new ConflictException(MajorErrorCode.MAJOR_NAME_DUPLICATED));
        when(majorRepository.findByName(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.createMajors(List.of("산업공학"), 1L, "ADMIN"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("자격증 — 생성과 동명 충돌 재조회가 전공과 같은 규칙으로 동작한다")
    void certificatesMirrorMajors() {
        when(certificateCommandUseCase.create(new CreateCertificateCommand("정보처리기사", "ADMIN")))
                .thenReturn(new CertificateResult(70L, "정보처리기사"));
        when(certificateCommandUseCase.create(new CreateCertificateCommand("SQLD", "ADMIN")))
                .thenThrow(new ConflictException(CertificateErrorCode.CERT_NAME_DUPLICATED));
        when(certificateRepository.findByName("SQLD", 1L))
                .thenReturn(Optional.of(Certificate.restore(8L, 1L, "SQLD", LocalDateTime.now())));

        Map<String, Long> ids = adapter.createCertificates(List.of("정보처리기사", "SQLD"), 1L, "ADMIN");

        assertThat(ids).containsExactly(Map.entry("정보처리기사", 70L), Map.entry("SQLD", 8L));
    }
}
