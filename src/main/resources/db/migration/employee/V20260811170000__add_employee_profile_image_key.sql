-- 프로필 사진 기능: 사원별 프로필 사진의 S3 키를 저장한다.
--   · 업로드/삭제(본인만)  : auth 마이페이지  — .ai/api/auth.md §5-1 · §5-2
--   · 아바타 서빙(누구나)   : employee 도메인 — .ai/api/employee.md §10
-- 값이 NULL 이면 사진 없음 → 프론트가 이니셜/기본 아바타를 그린다.
-- 키 형식: profile-images/{userId}/{UUID}.{ext} (실제 S3 객체는 이 키로 저장, 컬럼엔 키만).
ALTER TABLE employee
  ADD COLUMN profile_image_key VARCHAR(512) NULL
    COMMENT '프로필 사진 S3 키 (NULL=사진 없음)'
    AFTER phone;
