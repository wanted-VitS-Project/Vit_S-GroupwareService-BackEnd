package com.group3.vitamins.bidding.bidsummary.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(BiddingWorkerAuthProperties.class)
public class BiddingInternalSecurityConfig {

    private final BiddingWorkerAuthProperties properties;

    @Bean
    @Order(2)
    public SecurityFilterChain biddingInternalSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {
        return http
                .securityMatcher("/internal/v1/bidding/**")
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/internal/v1/bidding/**"
                ))
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                ))
                .addFilterBefore(
                        new BiddingWorkerTokenAuthenticationFilter(properties),
                        AuthorizationFilter.class
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("BIDDING_WORKER")
                )
                .build();
    }
}