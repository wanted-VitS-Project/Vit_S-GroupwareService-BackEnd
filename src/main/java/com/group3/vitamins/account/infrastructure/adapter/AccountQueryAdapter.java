package com.group3.vitamins.account.infrastructure.adapter;

import com.group3.vitamins.account.application.port.AccountQueryPort;
import com.group3.vitamins.account.application.result.AccountTargetRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link AccountQueryPort} 의 MyBatis 어댑터. 실제 SQL 은 {@link AccountQueryMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class AccountQueryAdapter implements AccountQueryPort {

    private final AccountQueryMapper accountQueryMapper;

    @Override
    public Optional<AccountTargetRow> findTarget(String userId) {
        return accountQueryMapper.findTarget(userId);
    }

    @Override
    public List<AccountTargetRow> findTargets(Collection<String> userIds) {
        return accountQueryMapper.findTargets(userIds);
    }
}
