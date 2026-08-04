package com.group3.vitamins.account.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 재설정된 비밀번호를 <b>한 트랜잭션</b>으로 일괄 반영한다.
 *
 * <p>🚨 <b>해싱은 이 트랜잭션 밖에서 끝낸 뒤</b> 인코딩된 값만 넘겨받는다. 64MB Argon2 해시 N 회를
 * 트랜잭션 안에서 돌리면 그동안 DB 커넥션을 쥐고 있어 커넥션 풀이 마른다 (STATE `🔐 인증·보안` 참고).
 *
 * <p>오케스트레이션({@link AccountPasswordResetService})과 <b>별도 빈</b>이어야 한다 —
 * 같은 빈 안에서 호출하면 프록시를 안 거쳐 {@code @Transactional} 이 무시된다(자기 호출).
 */
@Component
@RequiredArgsConstructor
public class AccountPasswordUpdater {

    private final AccountJpaRepository accountRepository;

    /**
     * @param encodedByUserId 사번 → 인코딩된 새 비밀번호. 호출 전에 존재·대상 검증이 끝나 있어야 한다.
     */
    @Transactional
    public void applyResets(Map<String, String> encodedByUserId) {
        List<AccountEntity> accounts = accountRepository.findAllByUserIdIn(encodedByUserId.keySet());
        if (accounts.size() != encodedByUserId.size()) {
            // 검증과 반영 사이에 계정이 사라진 경우. 전체를 롤백한다.
            throw new NotFoundException(AccountErrorCode.ACC_NOT_FOUND);
        }
        accounts.forEach(account -> account.resetPassword(encodedByUserId.get(account.getUserId())));
    }
}
