package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.certificate.application.command.CreateCertificateCommand;
import com.group3.vitamins.certificate.application.usecase.CertificateCommandUseCase;
import com.group3.vitamins.certificate.domain.model.Certificate;
import com.group3.vitamins.certificate.domain.repository.CertificateRepository;
import com.group3.vitamins.employee.application.port.QualificationMasterCreatePort;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.major.application.command.CreateMajorCommand;
import com.group3.vitamins.major.application.usecase.MajorCommandUseCase;
import com.group3.vitamins.major.domain.model.Major;
import com.group3.vitamins.major.domain.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link QualificationMasterCreatePort} 구현 — 전공/자격증 마스터를 <b>상대 도메인의 생성 유스케이스</b>로 만든다
 * (아키텍처 §2-2 "재사용할 로직이 있으면 유스케이스를 호출한다" — 이름 규칙·회사 UNIQUE·회사 스탬핑을 복제하지 않는다).
 *
 * <p>⚠️ 이 어댑터에는 {@code @Transactional} 을 걸지 않는다 — 마스터 하나가 한 트랜잭션이다.
 * 바깥 트랜잭션으로 묶으면 안의 {@code create} 가 {@code ConflictException} 을 던지는 순간 트랜잭션이 rollback-only 로 표시돼
 * 잡아서 재조회해도 커밋에서 {@code UnexpectedRollbackException} 이 난다. 마스터는 사원과 독립 생명주기(사원 없이 존재·삭제 권한은 마스터 관리)라
 * 부분 생성이 남아도 손실이 아니다(관리 화면에서 삭제 가능).
 *
 * <p>동명 충돌({@code *_NAME_DUPLICATED})은 두 경우다 — 검증~등록 사이 다른 관리자가 먼저 만든 경우 · DB 대소문자 무시 collation 으로
 * "sqld" 가 기존 "SQLD" 와 겹치는 경우. 둘 다 새로 만들지 않고 이름으로 재조회해 그 id 를 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualificationMasterCreateAdapter implements QualificationMasterCreatePort {

    private final MajorCommandUseCase majorCommandUseCase;
    private final MajorRepository majorRepository;
    private final CertificateCommandUseCase certificateCommandUseCase;
    private final CertificateRepository certificateRepository;

    @Override
    public Map<String, Long> createMajors(Collection<String> names, Long companyId, String actorRole) {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (String name : names) {
            Long id;
            try {
                id = majorCommandUseCase.create(new CreateMajorCommand(name, actorRole)).majorId();
            } catch (ConflictException e) {
                id = majorRepository.findByName(name, companyId).map(Major::getMajorId)
                        .orElseThrow(() -> new IllegalStateException("전공 동명 충돌 후 재조회 실패 - name=" + name, e));
                log.info("엑셀 자동 생성 - 전공 동명 존재, 기존 참조 name={} id={}", name, id);
            }
            ids.put(name, id);
        }
        return ids;
    }

    @Override
    public Map<String, Long> createCertificates(Collection<String> names, Long companyId, String actorRole) {
        Map<String, Long> ids = new LinkedHashMap<>();
        for (String name : names) {
            Long id;
            try {
                id = certificateCommandUseCase.create(new CreateCertificateCommand(name, actorRole)).certificateId();
            } catch (ConflictException e) {
                id = certificateRepository.findByName(name, companyId).map(Certificate::getCertificateId)
                        .orElseThrow(() -> new IllegalStateException("자격증 동명 충돌 후 재조회 실패 - name=" + name, e));
                log.info("엑셀 자동 생성 - 자격증 동명 존재, 기존 참조 name={} id={}", name, id);
            }
            ids.put(name, id);
        }
        return ids;
    }
}
