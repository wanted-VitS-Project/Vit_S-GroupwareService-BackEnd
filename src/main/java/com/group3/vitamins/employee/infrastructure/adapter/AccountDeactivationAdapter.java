package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.employee.application.port.AccountDeactivationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link AccountDeactivationPort} 의 JPA 어댑터. 퇴사 트랜잭션 안에서 계정을 {@code INACTIVE} 로 바꾼다.
 *
 * <p>계정이 없으면(정상 데이터라면 사원엔 계정이 1:1로 있다) 조용히 건너뛴다 — 퇴사는 사원 상태가 본질이고
 * 계정 비활성화는 부수 효과라, 계정 부재로 퇴사를 실패시키지 않는다.
 */
@Component
@RequiredArgsConstructor
public class AccountDeactivationAdapter implements AccountDeactivationPort {

    private final AccountJpaRepository accountJpaRepository;

    @Override
    public void deactivate(String userId) {
        accountJpaRepository.findByUserId(userId).ifPresent(account -> {
            account.changeStatus("INACTIVE");
            accountJpaRepository.saveAndFlush(account);
        });
    }
}
