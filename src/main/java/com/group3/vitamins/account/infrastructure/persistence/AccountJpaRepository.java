package com.group3.vitamins.account.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByUserId(String userId);
}
