package com.group3.vitamins.global.presentation.api.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class RequesterRole {

    private static final String ROLE_PREFIX = "ROLE_";

    private RequesterRole() {
    }

    /** 세션 권한(ROLE_ADMIN 형태)에서 전역 role 문자열을 꺼낸다. 없으면 빈 문자열. */
    public static String from(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .findFirst()
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .orElse("");
    }
}
