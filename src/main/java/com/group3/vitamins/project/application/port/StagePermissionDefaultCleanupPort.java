package com.group3.vitamins.project.application.port;

/**
 * 참여자 제거 시 스테이지 권한 기본값을 정리하는 아웃바운드 포트 (STG-004).
 *
 * <p>🚨 이걸 빼먹으면 <b>뺐던 사람을 재초대하고 그 스테이지에 스텝을 만드는 순간 죽은 기본값이 되살아난다.</b>
 * 2026-08-06 의 스텝 권한 누수와 같은 계열이다.
 */
public interface StagePermissionDefaultCleanupPort {

    void deleteByProjectIdAndUserId(Long projectId, String userId);
}
