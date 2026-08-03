package com.group3.vitamins.auth.application;

import com.group3.vitamins.account.infrastructure.persistence.AccountEntity;
import com.group3.vitamins.account.infrastructure.persistence.AccountJpaRepository;
import com.group3.vitamins.auth.domain.exception.AuthErrorCode;
import com.group3.vitamins.auth.infrastructure.persistence.AuthQueryMapper;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 로그인 실패 기록이 <b>바깥 트랜잭션 롤백에도 살아남는지</b> 검증한다.
 *
 * <p>이 테스트가 존재하는 이유: {@code AuthService.login()} 은 실패 카운트를 올린 직후 예외를 던진다.
 * 기록이 같은 트랜잭션에 있으면 전부 롤백돼 <b>5회 실패 잠금이 영원히 걸리지 않는다.</b>
 * 순수 Mockito 테스트({@code AuthServiceTest})는 트랜잭션 프록시가 없어 이 결함을 통과시킨다 —
 * 실제로 PR #95 리뷰 전까지 그렇게 통과했다.
 *
 * <p>클래스 레벨 {@code NOT_SUPPORTED} 로 <b>테스트 트랜잭션을 끈다.</b> 안 끄면 테스트가 감싼
 * 트랜잭션 안에서 전부 돌아 커밋 여부를 볼 수 없다.
 *
 * <p>DB 는 H2 다. MySQL 전용 Flyway 마이그레이션은 돌지 않으므로 끄고, 스키마는 엔티티에서 만든다.
 * 여기서 보는 건 스키마 정합성이 아니라 <b>트랜잭션 경계</b>다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:login-failure-tx;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "security.login.lock-threshold=5",
        "security.login.lock-duration=10m",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuthService.class, LoginFailureRecorder.class, LoginFailureTransactionTest.TestBeans.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("로그인 실패 기록의 트랜잭션 경계")
class LoginFailureTransactionTest {

    private static final String USER_ID = "EMP001";
    private static final String STORED = "stored-hash";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 3, 9, 0);

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountJpaRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(AccountEntity.issue(USER_ID, STORED, "MEMBER"));
    }

    @Test
    @DisplayName("비밀번호가 틀려 예외가 나가도 실패 카운트는 커밋된다")
    void failureCountSurvivesRollback() {
        assertThatThrownBy(() -> authService.login(USER_ID, "wrong"))
                .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                        .isEqualTo(AuthErrorCode.AUTH_LOGIN_FAILED));

        // 같은 트랜잭션에 두면 여기서 0 이 나온다 — 잠금이 영원히 안 걸리는 상태
        assertThat(reload().getLoginFailCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("임계치(5회)에 닿으면 실제로 잠기고 그 상태가 DB 에 남는다")
    void locksAfterThresholdAndPersists() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> authService.login(USER_ID, "wrong"))
                    .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                            .isEqualTo(AuthErrorCode.AUTH_LOGIN_FAILED));
        }
        assertThat(reload().getLoginFailCount()).isEqualTo(4);

        // 5회째 — 잠긴다
        assertThatThrownBy(() -> authService.login(USER_ID, "wrong"))
                .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                        .isEqualTo(AuthErrorCode.AUTH_ACCOUNT_LOCKED));

        AccountEntity locked = reload();
        assertThat(locked.getLockedUntil()).isEqualTo(NOW.plusMinutes(10));
        // 잠금과 함께 카운트를 0으로 되돌린다 — 해제 직후 1회만 틀려도 다시 잠기면 안 된다
        assertThat(locked.getLoginFailCount()).isZero();

        // 잠긴 뒤에는 올바른 비밀번호여도 막힌다
        assertThatThrownBy(() -> authService.login(USER_ID, "right"))
                .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                        .isEqualTo(AuthErrorCode.AUTH_ACCOUNT_LOCKED));
    }

    private AccountEntity reload() {
        return accountRepository.findByUserId(USER_ID).orElseThrow();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        /** Argon2 는 1회 0.15초라 반복 로그인 테스트에 쓸 수 없다. 검증 대상도 아니다. */
        @Bean
        PasswordEncoder passwordEncoder() {
            return new PasswordEncoder() {
                @Override
                public String encode(CharSequence rawPassword) {
                    return "right".contentEquals(rawPassword) ? STORED : "other-hash";
                }

                @Override
                public boolean matches(CharSequence rawPassword, String encodedPassword) {
                    return encode(rawPassword).equals(encodedPassword);
                }
            };
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }

        /** 실패 경로는 프로필을 조회하지 않는다. 생성자를 채우기 위한 목이다. */
        @Bean
        AuthQueryMapper authQueryMapper() {
            return Mockito.mock(AuthQueryMapper.class);
        }
    }
}
