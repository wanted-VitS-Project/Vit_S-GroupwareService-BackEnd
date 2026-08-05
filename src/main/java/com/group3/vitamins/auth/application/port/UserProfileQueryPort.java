package com.group3.vitamins.auth.application.port;

import com.group3.vitamins.auth.application.result.UserProfileRow;

import java.util.Optional;

/**
 * 인증 화면용 프로필 조회 아웃바운드 포트. <b>계정·사원·부서·직급 4개 애그리거트를 가로지르므로</b>
 * MyBatis 로 읽는다 (실제 조회는 {@code infrastructure/adapter} 어댑터가 처리한다).
 * 쓰기는 공유 인증 엔티티 {@code AccountEntity}(JPA)가 맡는다.
 */
public interface UserProfileQueryPort {

    /** 로그인 응답 · 내 정보 조회에 필요한 값 전부. 없으면 빈 값(세션은 살아 있는데 계정이 사라진 경우). */
    Optional<UserProfileRow> findProfile(String userId);
}
