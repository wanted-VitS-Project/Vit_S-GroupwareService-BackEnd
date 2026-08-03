package com.group3.vitamins.account.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByUserId(String userId);

    /**
     * 실패 카운트를 올리기 <b>직전</b>에만 쓰는 잠금 조회 (`SELECT ... FOR UPDATE`).
     *
     * <p>락이 없으면 동시 요청 5건이 같은 `login_fail_count` 를 읽고 각자 +1 을 써서
     * 카운트가 1 로 남는다. 병렬 브루트포스로 잠금 임계치를 통째로 우회할 수 있다.
     *
     * <p>⚠️ 호출은 {@link com.group3.vitamins.auth.application.LoginFailureRecorder} 안에서만 한다.
     * 바깥 트랜잭션이 이 락을 쥔 채 {@code REQUIRES_NEW} 로 같은 행을 다시 잠그면 자기 자신과 교착한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.userId = :userId")
    Optional<AccountEntity> findByUserIdForUpdate(@Param("userId") String userId);
}
