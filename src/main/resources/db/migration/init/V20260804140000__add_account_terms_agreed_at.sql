-- 약관 동의 시각. NULL = 미동의 → 최초 로그인 시 약관 동의를 강제한다 (auth.md §5 · §6-7).
-- must_change_password 만으로는 "최초 로그인"과 "재설정 후 로그인"을 구분할 수 없어 별도 신호가 필요하다.
-- ADMIN 계정은 코드에서 게이트를 스킵하므로 이 값과 무관하다.
ALTER TABLE account
    ADD COLUMN terms_agreed_at DATETIME NULL COMMENT '약관 동의 시각 (NULL=미동의)' AFTER must_change_password;
