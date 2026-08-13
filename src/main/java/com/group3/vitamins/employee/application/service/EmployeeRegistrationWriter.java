package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.employee.application.port.AccountProvisioningPort;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.model.EmployeeCertificate;
import com.group3.vitamins.employee.domain.model.EmployeeEducation;
import com.group3.vitamins.employee.domain.repository.EmployeeCertificateRepository;
import com.group3.vitamins.employee.domain.repository.EmployeeEducationRepository;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사원 + 계정 + 학력/자격증을 <b>한 트랜잭션</b>으로 INSERT 하는 쓰기 경계 (`employee.md` §3 · 아키텍처 §2-2).
 *
 * <p>{@link EmployeeCommandService} 에서 분리한 이유 — 비밀번호 해싱(Argon2 64MB)은 트랜잭션 밖에서 끝내야
 * DB 커넥션을 오래 잡지 않는다. 서비스는 해시를 마친 뒤 이 컴포넌트를 호출해 <b>쓰기만</b> 트랜잭션에 담는다
 * (self-invocation 은 프록시를 우회해 트랜잭션이 안 걸리므로 별도 빈으로 둔다).
 *
 * <p>사원 저장은 어댑터에서 {@code saveAndFlush} 라 PK·UNIQUE 위반이 이 안에서 즉시
 * {@code DataIntegrityViolationException} 으로 터진다 → 트랜잭션 롤백 후 호출자가 409 로 변환한다.
 * 학력/자격증은 사원 flush 뒤에 저장하므로 {@code user_id} FK 가 이미 성립한다(전공·자격증 마스터는 서비스가 선검증).
 */
@Component
@RequiredArgsConstructor
public class EmployeeRegistrationWriter {

    private final EmployeeRepository employeeRepository;
    private final AccountProvisioningPort accountProvisioningPort;
    private final EmployeeEducationRepository employeeEducationRepository;
    private final EmployeeCertificateRepository employeeCertificateRepository;

    @Transactional
    public void register(Employee employee, String role, String encodedPassword,
                         List<EmployeeEducation> educations, List<EmployeeCertificate> certificates) {
        employeeRepository.save(employee);
        accountProvisioningPort.provision(employee.getUserId(), role, encodedPassword);
        employeeEducationRepository.saveAll(educations);
        employeeCertificateRepository.saveAll(certificates);
    }
}
