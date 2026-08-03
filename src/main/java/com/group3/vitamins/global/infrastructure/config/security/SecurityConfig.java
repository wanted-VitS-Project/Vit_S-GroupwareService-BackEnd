package com.group3.vitamins.global.infrastructure.config.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.DispatcherType;
import com.group3.vitamins.auth.infrastructure.web.PasswordResetGateFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 인증 = <b>서버측 세션 + HttpOnly 쿠키</b> (Spring Session · Redis).
 *
 * <p>JWT 를 쓰지 않는 이유는 이 시스템이 <b>매 요청 서버 상태를 봐야</b> 하기 때문이다.
 * <ul>
 *   <li>ADMIN 이 role · page_permission 을 수시로 바꾼다 → 즉시 반영돼야 한다
 *       (JWT 는 클레임이 발급 시점 스냅샷이라 재로그인을 강제해야 한다)</li>
 *   <li>계정 잠금 · 비활성화 시 기존 접속을 즉시 끊어야 한다</li>
 * </ul>
 * 매 요청 Redis 를 조회할 거라면 JWT 의 무상태 이점은 성립하지 않고, 재발급 API·프론트 인터셉터
 * 비용만 남는다. 상세 판단은 {@code .ai/local/STATE.md} 의 `🔐 인증·보안 확정값` 참고.
 */
@Slf4j
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(Argon2Properties.class)
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final Argon2Properties argon2;

    /** 허용 오리진(콤마 구분). 배포는 CORS_ALLOWED_ORIGINS 로 주입한다. */
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @PostConstruct
    void logSecurityParameters() {
        // 사이징 근거를 기동 로그에 남긴다. 파라미터를 바꿔 배포했을 때 무엇이 적용됐는지 바로 보인다.
        log.info("Argon2id m={}KB t={} p={} · 세마포어 permit={} (예상 피크 힙 약 {}MB) · 대기 로그인 {} / 일괄 {}",
                argon2.memoryKb(), argon2.iterations(), argon2.parallelism(),
                argon2.permits(), argon2.estimatedPeakHeapMb(), argon2.loginWait(), argon2.bulkWait());
    }

    /**
     * 비밀번호 인코더.
     *
     * <p>⚠️ {@link Argon2PasswordEncoder} 를 직접 빈으로 노출하지 않는다.
     * {@link ThrottledPasswordEncoder} 로 감싼 것만 주입되게 해야 동시 실행 제한을 우회할 수 없다.
     *
     * <p>생성자 인자 순서 주의: (saltLength, hashLength, parallelism, memory, iterations)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        Argon2PasswordEncoder argon2Encoder = new Argon2PasswordEncoder(
                argon2.saltLength(),
                argon2.hashLength(),
                argon2.parallelism(),
                argon2.memoryKb(),
                argon2.iterations()
        );
        return new ThrottledPasswordEncoder(argon2Encoder, argon2.permits(), argon2.loginWait());
    }

    /**
     * 세션에 SecurityContext 를 저장·복원한다.
     *
     * <p>폼 로그인을 쓰지 않으므로 로그인 성공 시 {@code AuthSessionManager} 가 이 빈으로
     * <b>직접 저장</b>한다. Spring Security 6 은 필터가 자동 저장해주지 않는다.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * 초기 비밀번호 변경 전에는 다른 기능을 막는 게이트 (`ACC-006`).
     *
     * <p>인가 필터 <b>뒤</b>에 둔다 — 인증조차 안 된 요청은 Security 가 먼저 401 로 끊어야 한다.
     */
    @Bean
    public PasswordResetGateFilter passwordResetGateFilter(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        return new PasswordResetGateFilter(resolver);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository securityContextRepository,
                                                   PasswordResetGateFilter passwordResetGateFilter)
            throws Exception {
        http
                // 쿠키 인증인데 CSRF 를 끄는 근거는 SameSite=Lax 다. 브라우저가 cross-site 요청에
                // 쿠키를 싣지 않으므로 CSRF 방어가 성립한다.
                // 🚨 SameSite=None 으로 바꾸는 순간 이 근거가 사라진다. 그때는 CSRF 토큰을 반드시 도입할 것.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        // 로그인 시점에만 세션이 생긴다. 비인증 요청은 세션을 만들지 않는다.
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // ASYNC/ERROR 디스패치는 최초 REQUEST 에서 이미 인가를 마친 재진입이다.
                        // 막으면 에러 페이지·비동기 응답이 깨진다.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // API 문서 — 운영 프로필에서는 springdoc 자체가 꺼져 있어 노출되지 않는다
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // 헬스체크. 그 외 actuator 엔드포인트는 열지 않는다
                        .requestMatchers("/actuator/health").permitAll()

                        // 로그인만 비인증 허용. 나머지 인증 API(로그아웃·내정보·비밀번호변경)는 세션이 필요하다.
                        // 경로 출처: .ai/api/auth.md (노션 확정)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                // 인가가 끝난 뒤에 건다 — 미인증 요청은 401 이 먼저 나가야 한다
                .addFilterAfter(passwordResetGateFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // 세션 쿠키를 주고받으므로 필수. 이게 true 면 allowedOrigins 에 "*" 를 쓸 수 없다.
        // 프론트도 fetch 에 credentials: 'include' 를 켜야 한다.
        config.setAllowCredentials(true);
        // preflight 캐시. 없으면 브라우저 기본값(약 5초)이라 POST·PATCH 마다 OPTIONS 가 한 번 더 나간다.
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
