package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.employee.application.port.AccountProvisioningPort;
import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사원 + 계정을 <b>한 트랜잭션</b>으로 INSERT 하는 쓰기 경계 (`employee.md` §3 · 아키텍처 §2-2).
 *
 * <p>{@link EmployeeCommandService} 에서 분리한 이유 — 비밀번호 해싱(Argon2 64MB)은 트랜잭션 밖에서 끝내야
 * DB 커넥션을 오래 잡지 않는다. 서비스는 해시를 마친 뒤 이 컴포넌트를 호출해 <b>쓰기만</b> 트랜잭션에 담는다
 * (self-invocation 은 프록시를 우회해 트랜잭션이 안 걸리므로 별도 빈으로 둔다).
 *
 * <p>두 {@code save} 는 어댑터에서 {@code saveAndFlush} 라 PK·UNIQUE 위반이 이 안에서 즉시
 * {@code DataIntegrityViolationException} 으로 터진다 → 트랜잭션 롤백 후 호출자가 409 로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class EmployeeRegistrationWriter {

    private final EmployeeRepository employeeRepository;
    private final AccountProvisioningPort accountProvisioningPort;

    @Transactional
    public void register(Employee employee, String role, String encodedPassword) {
        employeeRepository.save(employee);
        accountProvisioningPort.provision(employee.getUserId(), role, encodedPassword);
    }
}
