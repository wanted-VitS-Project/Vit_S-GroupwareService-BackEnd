package com.group3.vitamins.vitamate.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

// 비타메이트 Python worker 내부 API만 담당하는 보안 설정
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(VitamateWorkerAuthProperties.class)
public class VitamateInternalSecurityConfig {

    private final VitamateWorkerAuthProperties properties;

    @Bean
    @Order(1)
    public SecurityFilterChain vitamateInternalSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/internal/v1/vitamate/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new VitamateWorkerTokenAuthenticationFilter(properties), AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("VITAMATE_WORKER"))
                .build();
    }
}