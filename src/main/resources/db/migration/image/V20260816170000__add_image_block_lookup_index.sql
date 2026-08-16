-- 블록 카드 미리보기 조회(ImageDetailMapper.findFirstImagesByImgBlockIds) 전용 인덱스.
--
-- 기존에는 FK(fk_image_img_block)가 만든 img_block_id 단일 인덱스뿐이라, deleted_at 과
-- order_index 는 행을 실제로 꺼낸 뒤에야 확인할 수 있었다. 그 탓에 블록별 최소 order_index 를
-- 구하는 상관 서브쿼리가 후보 행마다 그 블록의 이미지를 전부 훑는다(이미지 장수의 제곱에 비례).
--
-- 세 컬럼을 이 순서로 묶으면
--   ① img_block_id 로 seek → ② deleted_at IS NULL 로 좁힘 → ③ order_index 순으로 이미 정렬
-- 이 되어 MIN 이 첫 엔트리 조회로 끝나고, COUNT 도 행 접근 없이 인덱스만으로 센다.
--
-- img_id 는 명시하지 않는다 — InnoDB 보조 인덱스는 PK 를 자동으로 뒤에 붙인다.
-- 기존 FK 인덱스는 그대로 둔다 — 이 인덱스가 prefix 로 덮지만, 제거는 별개 판단이다.
ALTER TABLE image
    ADD INDEX idx_image_block_active_order (img_block_id, deleted_at, order_index);
