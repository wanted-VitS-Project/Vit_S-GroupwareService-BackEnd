package com.group3.vitamins.auth.infrastructure.adapter;

import com.group3.vitamins.auth.application.port.UserProfileQueryPort;
import com.group3.vitamins.auth.application.result.UserProfileRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link UserProfileQueryPort} 의 MyBatis 어댑터. 실제 SQL 은 {@link AuthQueryMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class UserProfileQueryAdapter implements UserProfileQueryPort {

    private final AuthQueryMapper authQueryMapper;

    @Override
    public Optional<UserProfileRow> findProfile(String userId) {
        return authQueryMapper.findProfile(userId);
    }
}
