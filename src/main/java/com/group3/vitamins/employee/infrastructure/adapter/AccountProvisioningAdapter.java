package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.employee.application.port.AccountProvisioningPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link AccountProvisioningPort} 의 JPA 어댑터. 사원 등록 트랜잭션 안에서 계정을 발급한다.
 *
 * <p>account 의 공유 엔티티 팩토리 {@link AccountEntity#issue}(status=ACTIVE·mustChangePassword=true)로 만든다.
 * {@code saveAndFlush} 로 즉시 반영해 UNIQUE({@code uk_account_user_id}) 위반을 커밋까지 미루지 않고 드러낸다
 * — 등록 유스케이스가 사번 중복(409)으로 변환할 수 있게 한다.
 */
@Component
@RequiredArgsConstructor
public class AccountProvisioningAdapter implements AccountProvisioningPort {

    private final AccountJpaRepository accountJpaRepository;

    @Override
    public void provision(String userId, String role, String encodedPassword) {
        accountJpaRepository.saveAndFlush(AccountEntity.issue(userId, encodedPassword, role));
    }
}
