-- =====================================================================
-- 멀티테넌트 Phase 0 · S0 — 회사(테넌트) 테이블
-- =====================================================================
-- 모든 격리 대상 데이터가 "어느 회사 것인지" 가리키는 루트 오브 루트.
-- 기본회사(company_id=1)를 여기서 시드해야, 다음 마이그레이션(S1)의
-- 기존 데이터 백필이 참조할 대상이 존재한다.

CREATE TABLE company (
  company_id   BIGINT       NOT NULL AUTO_INCREMENT              COMMENT '회사(테넌트) 번호',
  name         VARCHAR(100) NOT NULL                            COMMENT '회사명',
  company_code VARCHAR(30)  NOT NULL                            COMMENT 'URL-safe 회사 코드',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '생성일',
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',
  PRIMARY KEY (company_id),
  UNIQUE KEY uk_company_code (company_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회사(테넌트)';

-- 기존 단일 테넌트 데이터 전부의 소속이 될 기본 회사.
-- company_id 를 1 로 못 박아 S1 백필(UPDATE ... SET company_id = 1)이 참조한다.
INSERT INTO company (company_id, name, company_code)
VALUES (1, '기본회사', 'DEFAULT');
