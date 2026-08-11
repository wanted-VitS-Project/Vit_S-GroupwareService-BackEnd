-- 컬럼을 지우기 전에 기존 값을 보존한다. additional_instruction이 실제 사용자 요청 전문을
-- 담고 있었으므로 그 의미를 그대로 잇는 prompt로 옮긴다(prompt는 이전에는 요약만 담던 컬럼).
-- prompt_template_version은 분석당 단일 값이었지만 새 모델은 카테고리별로 나뉘어 있어
-- 안전하게 매핑할 대상이 없다 — 과거 분석 조회 응답에서만 templateVersions=[]로 남는다.
UPDATE vitamate_analysis
   SET prompt = additional_instruction
 WHERE additional_instruction IS NOT NULL
   AND additional_instruction <> '';

-- 최종 프롬프트와 분석별 템플릿 스냅샷으로 대체된 중복 컬럼을 제거한다.
ALTER TABLE vitamate_analysis
    DROP COLUMN additional_instruction,
    DROP COLUMN prompt_template_version;
