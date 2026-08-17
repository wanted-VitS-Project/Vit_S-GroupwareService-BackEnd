-- =====================================================================
-- 15. 문구 전면 재작성 — AI 말투 제거
-- ---------------------------------------------------------------------
-- 전수조사에서 걸러낸 것:
--   ① 메타 발언 — 문서가 자기 자신(블록·스텝·데모)을 설명한다. 실무 문서엔 없다.
--      "송부용 스텝 타입을 따로 만들지 않았다" / "더미값이다" / "회차마다 스텝을 만들지 않고"
--   ② 사람이 안 쓰는 제목 — "막힌 지점" "결론·미확정 항목" "AI 결과 — 사람 확정본"
--   ③ 가운뎃점 압축 — "발주·입고·검품" 처럼 명사를 억지로 붙여 줄인 제목
--   ④ 줄표(—) 남발 — 한국어 실무 문서에서 이렇게 안 쓴다
--   ⑤ "(가정)" 딱지 — 실무자는 숫자에 이런 주석을 안 단다
--
-- 되돌리기 없음. 실행 전 백업하려면:
--   mysqldump ... vitaS project stage step block text checklist image file_version issue > before.sql
-- =====================================================================

-- =====================================================================
-- 1. 프로젝트 설명
-- =====================================================================
UPDATE project SET description=
'무신사 스토어에 26 S/S 상품을 위탁판매로 넣고 시즌 운영까지 맡는 건이다. 입점 검토부터 상품 등록, 발주, 판매 운영, 월 정산까지 한 프로젝트에서 본다.'
WHERE project_id=9001;

UPDATE project SET description=
'25 F/W 시즌 무신사 운영 건. 끝난 프로젝트이고, 이번 시즌 기준을 잡을 때 수치를 참고한다.'
WHERE project_id=9002;

UPDATE project SET description=
'무신사 말고 채널을 더 늘릴지 보다가 접었다. 수수료가 높고 심사가 길어서 26 F/W 때 다시 보기로 했다.'
WHERE project_id=9003;

-- =====================================================================
-- 2. 스테이지 / 스텝 이름
-- =====================================================================
UPDATE stage SET name='입점 신청과 심사' WHERE stage_id=9002;
UPDATE stage SET name='입점 세팅'        WHERE stage_id=9003;
UPDATE stage SET name='26 S/S 시즌 운영' WHERE stage_id=9004;
UPDATE stage SET name='시즌 결산'        WHERE stage_id=9006;

UPDATE step SET name='입점 채널 조사'          WHERE step_id=9001;
UPDATE step SET name='사업성 검토와 추진 결재' WHERE step_id=9002;
UPDATE step SET name='입점 제출 자료 준비'     WHERE step_id=9003;
UPDATE step SET name='제출 자료 검토와 신청'   WHERE step_id=9004;
UPDATE step SET name='계약 체결과 계정 등록'   WHERE step_id=9006;
UPDATE step SET name='상품 등록과 오픈'        WHERE step_id=9007;
UPDATE step SET name='1차 발주와 입고'         WHERE step_id=9008;
UPDATE step SET name='1차 판매 운영'           WHERE step_id=9009;
UPDATE step SET name='2차 발주와 입고'         WHERE step_id=9010;
UPDATE step SET name='2차 판매 운영'           WHERE step_id=9011;
UPDATE step SET name='3차 발주와 입고'         WHERE step_id=9012;
UPDATE step SET name='3차 판매 운영'           WHERE step_id=9013;
UPDATE step SET name='월 정산'                 WHERE step_id=9014;
UPDATE step SET name='시즌 결산과 수익성 분석' WHERE step_id=9015;

-- =====================================================================
-- 3. 블록 제목
-- =====================================================================
UPDATE block SET title='입점 자격 확인'        WHERE block_id=9002;
UPDATE block SET title='카테고리 조사 화면'    WHERE block_id=9005;
UPDATE block SET title='오픈 일정 역산'        WHERE block_id=9006;
UPDATE block SET title='검토 결론'             WHERE block_id=9007;
UPDATE block SET title='AI 사업성 검토'        WHERE block_id=9008;
UPDATE block SET title='AI 검토 결과 확인'     WHERE block_id=9010;
UPDATE block SET title='결재 결과'             WHERE block_id=9014;
UPDATE block SET title='담당자 배정'           WHERE block_id=9015;
UPDATE block SET title='스타일별 판매가'       WHERE block_id=9017;
UPDATE block SET title='제출 자료'             WHERE block_id=9018;
UPDATE block SET title='제품컷과 룩북'         WHERE block_id=9019;
UPDATE block SET title='수수료 실부담'         WHERE block_id=9020;
UPDATE block SET title='자료 작성 현황'        WHERE block_id=9021;
UPDATE block SET title='이미지와 사이즈표 검토' WHERE block_id=9023;
UPDATE block SET title='서류와 작업지시서 검토' WHERE block_id=9024;
UPDATE block SET title='수정 전후 비교'        WHERE block_id=9026;
UPDATE block SET title='제출 내역'             WHERE block_id=9028;
UPDATE block SET title='검토 범위'             WHERE block_id=9103;
UPDATE block SET title='승인 직후 처리할 것'   WHERE block_id=9030;
UPDATE block SET title='계약과 계정 등록'      WHERE block_id=9036;
UPDATE block SET title='불리한 조항 검토'      WHERE block_id=9037;
UPDATE block SET title='계약 서류'             WHERE block_id=9038;
UPDATE block SET title='계정 등록 반려 건'     WHERE block_id=9041;
UPDATE block SET title='등록 대상과 규칙'      WHERE block_id=9042;
UPDATE block SET title='업로드 반려 이력'      WHERE block_id=9045;
UPDATE block SET title='오픈 설정'             WHERE block_id=9048;
UPDATE block SET title='발주 수량 결정'        WHERE block_id IN (9049,9063,9077);
UPDATE block SET title='발주와 검품 확인'      WHERE block_id IN (9050,9064,9078);
UPDATE block SET title='발주 서류'             WHERE block_id IN (9052,9066,9080);
UPDATE block SET title='공장 배분'             WHERE block_id=9053;
UPDATE block SET title='입고와 검품 결과'      WHERE block_id IN (9055,9069,9081);
UPDATE block SET title='판매 운영 점검'        WHERE block_id IN (9057,9071,9083);
UPDATE block SET title='기획전과 쿠폰'         WHERE block_id IN (9058,9072);
UPDATE block SET title='랭킹과 클레임'         WHERE block_id=9060;
UPDATE block SET title='문의와 반품'           WHERE block_id IN (9061,9075,9085);
UPDATE block SET title='개선 과제'             WHERE block_id=9062;
UPDATE block SET title='판매 현황 화면'        WHERE block_id=9074;
UPDATE block SET title='정산 기준'             WHERE block_id=9087;
UPDATE block SET title='회차별 확인 사항'      WHERE block_id=9091;
UPDATE block SET title='정산서 대조 결과'      WHERE block_id=9092;
UPDATE block SET title='정산 서류'             WHERE block_id=9093;
UPDATE block SET title='입금과 계산서 화면'    WHERE block_id=9094;
UPDATE block SET title='26 F/W 진행 여부'      WHERE block_id=9100;

-- =====================================================================
-- 4. 본문 (TEXT)
--    block_id 로 지정한다. txt_id 직접 참조는 매핑이 틀리면 조용히 엉뚱한 블록을 덮는다.
-- =====================================================================

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='후보 3사를 놓고 봤다.

- **무신사** 20~30대, 캐주얼 강세, 수수료 13~18%, 심사 난이도 중간
- **29CM** 25~35대 여성, 컨템포러리, 20~25%, 난이도 높음
- **W컨셉** 25~35대 여성, 디자이너, 20~27%, 난이도 높음

우먼 컨템포러리만 보면 29CM 과 W컨셉이 강하다. 다만 수수료가 높고 심사가 길다. 첫 채널은 물량을 태울 수 있는 곳이어야 해서 무신사를 먼저 본다.'
WHERE b.block_id=9003;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**거래 조건**
판매수수료 15%, 정산은 익월 10일, 반품은 익월 정산에서 상계한다.

**일정**
오픈을 2026-02-16 으로 잡고 역산했다.

- 촬영 12-22
- 상품등록 마감 01-23
- 검수 01-26~30
- 초도 입고 02-05

**생산 공수**
12스타일 3,400장이면 원단 20일에 봉제 35일이 걸린다. 발주는 늦어도 **2025-12-28** 까지 넣어야 한다.'
WHERE b.block_id=9006;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**무신사만 먼저 진행한다.** 29CM 은 26 F/W 때 다시 본다.
이현우 팀장 결정, 2025-11-14.

아직 못 정한 게 하나 있다. 쿠폰을 브랜드가 얼마나 부담하는지가 안 정해졌다. 판매가를 뽑으려면 이 숫자가 먼저 나와야 해서 MD 에게 다시 물어봐야 한다.'
WHERE b.block_id=9007;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='AI 가 지적한 3건을 하나씩 봤다.

- 상표권 등록이 안 끝났다 → **맞는 지적.** 승인 조건으로 붙을 가능성이 높다
- 쿠폰 분담률이 안 정해져 마진 오차가 있다 → **맞는 지적.** 일단 브랜드 50% 부담으로 보수적으로 잡는다
- 2월 물류 피크로 배송이 밀릴 수 있다 → **과한 걱정.** 우리 물량 3,400장이면 일 최대 출고 400건 캐파에 한참 못 미친다

김서연, 2025-12-01.'
WHERE b.block_id=9010;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='대표 3종으로 잡아본 마진이다.

- **오버핏 블레이저** 189,000원 / 원가 61,000원 → 마진 90,200원 (**47.7%**)
- **케이블 니트** 98,000원 / 원가 31,000원 → 마진 47,400원 (48.4%)
- **와이드 슬랙스** 79,000원 / 원가 24,500원 → 마진 38,700원 (49.0%)

시즌 전체로는 GMV 2.1억, 정산액 1.7억, 영업이익률 27% 로 본다.'
WHERE b.block_id=9011;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**승인** 2025-12-05. 상신부터 3영업일 걸렸다.

한 번 반려됐다. 11-29 에 한지훈 본부장이 마진 시뮬레이션에 반품율이 안 들어갔다고 돌려보냈다. v4 에 반품율 12% 를 반영해 v5 로 다시 올려 승인받았다.

대표 승인 조건이 하나 붙었다. 1차 발주를 원안의 **60% 로 줄이고** 나머지는 판매 보고 리오더로 대응하라는 것이다.'
WHERE b.block_id=9014;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**투입 인원**

- 김서연 주담당 70%
- 박준호 MD 커뮤니케이션 50%
- 이현우 검토와 결재 20%
- 정민아 룩북과 제품컷 60%
- 최동석 입고, 검품, 문의 응대 30%

정민아가 12-22~26 휴가라 룩북 촬영을 **12-18 로 당겼다.**'
WHERE b.block_id=9015;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='자사몰과 **같은 가격**으로 간다. 채널마다 값을 다르게 두면 자사몰 고객이 빠지고, 한번 벌어지면 되돌리기 어렵다.

대표 4종 판매가다. 괄호는 수수료 뺀 실수령액.

- 오버핏 블레이저 189,000 (160,650)
- 트렌치 코트 229,000 (194,650)
- 케이블 니트 98,000 (83,300)
- 와이드 슬랙스 79,000 (67,150)

12종 전체는 판매가 산출 시트에 있다.'
WHERE b.block_id=9017;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='계약서에 적힌 수수료와 실제로 나가는 돈이 다르다.

- 판매수수료 **15.0%**
- 쿠폰 분담 +2.4%p
- 반품 차감 환산 +1.0%p
- 기획전 무신사 부담분 −6.2%p

합치면 **실부담 12.2%** 정도다. 기획전에 얼마나 들어가느냐에 따라 달마다 달라진다.'
WHERE b.block_id=9020;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- 브랜드 소개서 v1, 김서연 작성 / 이현우 검수, 12-10
- 26SS 룩북 v1, 정민아 작성 / 김서연 검수, 12-11
- 판매가 산출 시트 v1, 박준호 작성 / 이현우 검수, 12-11'
WHERE b.block_id=9021;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**3건 나왔고 12-11 에 전부 조치했다.**

- VW-2601 사이즈표 누락 → 실측해서 추가 (정민아)
- 이미지 4장이 900×900 으로 규격 미달 → 1200×1200 재작업 (정민아)
- 소재 혼용률 합계가 98% → 면 62 / 폴리 38 로 정정 (박준호)'
WHERE b.block_id=9025;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- 발송 **김서연**
- 2025-12-12 14:20
- 무신사 파트너센터 웹 제출
- 수신 우먼컨템포러리팀 MD
- 접수번호 **MS-25-11842**'
WHERE b.block_id=9028;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='무신사 제출 가이드에 나온 4개 영역을 본다.

1. 이미지 규격
2. 사이즈표
3. 작업지시서
4. 권리 서류

검토는 이현우 팀장이 맡고, 지적사항은 전부 조치한 뒤 다시 본다.'
WHERE b.block_id=9103;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**승인 났다.**

12-12 접수해서 12-16 결과가 나왔다. 4영업일 걸렸다.

MD 코멘트는 브랜드 색이 분명하고 제품 이미지 완성도가 기준을 넘는다는 것이었다. 초도 물량과 사이즈 운영 계획이 구체적이라는 평도 있었다.'
WHERE b.block_id=9029;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- 아우터가 2종은 적다, 4종 이상으로 늘려달라
- 가격대가 같은 카테고리 평균보다 15% 낮다, 올릴 여지가 있다
- 초도는 3,000~4,000장 정도를 권한다

아우터는 26 F/W 기획에 반영할지 보고 있다. 가격은 자사몰과 같은 값으로 가기로 한 것과 부딪혀서 일단 보류했다.'
WHERE b.block_id=9031;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- 초도에 **최소 8스타일** 이상 등록할 것
- 상표권 **등록증을 2026-03 까지** 낼 것 (지금은 출원만 된 상태)

두 번째는 사업성 검토 때 리스크로 올렸던 게 그대로 조건이 돼서 돌아온 것이다. 등록 심사가 어디까지 갔는지 월 1회 확인한다.'
WHERE b.block_id=9034;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- 계약일 2025-12-19
- 수수료율 15%
- 정산 월 1회, 익월 10일
- 계약기간 1년, 자동갱신
- 해지는 30일 전 서면 통보
- 반품 귀책은 원안 그대로'
WHERE b.block_id=9035;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**2건 수정 요청해서 1건 받아들여졌다.**

수수료를 일방적으로 바꿀 수 있다는 조항. 30일 전에 서면으로 알리는 조건을 넣어달라고 했고 **반영됐다.**

반품을 전부 브랜드가 부담한다는 조항. 플랫폼 귀책분은 빼달라고 했지만 **받아들여지지 않아 원안 그대로 간다.**'
WHERE b.block_id=9037;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- 사업자번호 000-00-00000
- 정산 계좌 OO은행 ***-**-****12
- 예금주 주식회사 비타웨어
- 담당 조은비'
WHERE b.block_id=9039;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**정산 계좌 명의가 안 맞아 12-26 에 한 번 반려됐다.**

사업자등록증에는 "주식회사 비타웨어" 인데 계좌 예금주가 "비타웨어" 로만 등록돼 있었다.

은행에서 예금주 표기를 고치고 12-29 에 다시 냈다. **12-30 승인.**'
WHERE b.block_id=9041;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='12스타일에 컬러 2~3개, 사이즈 3~4개를 붙이면 **SKU 118개**가 된다.

**등록 규칙**

- 상품명 `[VITAWEAR] 품명 (컬러)`
- 옵션은 2단, 컬러 다음 사이즈
- 배송비 3,000원, 5만원 이상 무료
- 원산지 제조국 표기 필수'
WHERE b.block_id=9042;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**세 번 반려된 끝에 통과했다.**

- v2 원산지 코드 오류 (VNM → VN), 01-14
- v3 사이즈 옵션 중복 (M 이 두 번), 01-16
- v4 소재 혼용률 합계 불일치 5스타일, 01-19
- v5 부분 통과 103/118, 01-20
- **v6 전건 통과 118/118, 01-22**'
WHERE b.block_id=9045;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='118개 SKU 중 **5건 보류**됐다. 이미지 규격 3건, 사이즈표 2건.

01-28 다시 내서 **01-29 전건 통과.**'
WHERE b.block_id=9047;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- 오픈 **2026-02-16 예약**
- 노출 카테고리 우먼 > 아우터 / 니트
- 초기 진열은 신상품 순

초도 입고가 02-05 라 오픈까지 11일 여유가 있다. 입고가 사흘 이상 밀리면 오픈을 02-23 으로 미루기로 MD 와 미리 얘기해뒀다.'
WHERE b.block_id=9048;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='대표 승인 조건대로 원안에서 **40% 를 덜어냈다.**

원안 5,600장 → 발주 **3,400장**

남은 2,200장은 판매 데이터를 보고 4월에 리오더로 넣는다.'
WHERE b.block_id=9049;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**3,400장, 72,420,000원.** 평균 단가 21,300원.

주요 4종

- 오버핏 블레이저 280장, 10,640,000원, C공장
- 트렌치 코트 220장, 10,120,000원, C공장
- 케이블 니트 420장, 8,190,000원, A공장
- 와이드 슬랙스 380장, 5,890,000원, B공장

12종 전체는 발주서에 있다. 납기 02-03~05.'
WHERE b.block_id=9051;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='- A공장 니트 6종 1,860장
- B공장 우븐 4종 1,000장
- C공장 아우터 2종 540장

걱정되는 게 두 가지 있다.

C공장은 이번이 첫 거래다. 아우터는 단가가 높아서 불량이 나면 손실이 크니 중간 검품을 한 번 더 넣었다.

A공장은 12~1월에 타사 OEM 물량이 먼저 잡혀 있어 우리 라인이 뒤로 밀린다. 자체 브랜드를 우선 배정하겠다는 확답을 12-26 에 서면으로 받아뒀다.'
WHERE b.block_id=9053;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='예정은 02-05 였는데 **02-07 에 들어왔다.** 이틀 늦었다. C공장에서 원단 염색을 다시 하느라 밀렸다.

**수량 대조**

- 발주 3,400 / 입고 **3,362** / 차이 −38
- 불량 폐기 22장 (봉제 9, 오염 8, 사이즈 편차 5)
- 미납 16장은 재작업해서 02-14 입고

합격률 **99.3%**, 판매 가능 수량 3,362장.'
WHERE b.block_id=9055;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**오픈 첫 주** (02-16~22)
GMV 21,400,000원, 주문 312건, 객단가 68,590원, 판매율 18.6%

**2월 마감**
GMV **68,400,000원.** 이 금액이 1차 정산의 구매확정 매출 기준이 된다.'
WHERE b.block_id=9056;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**기획전**
26SS 신상 기획전에 신청해서 02-14 에 선정됐다. 노출 기간은 02-16~29.

**쿠폰**

- 신규가입 10%, 브랜드가 절반 부담
- 기획전 5%, 무신사가 전액 부담'
WHERE b.block_id=9058;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='2월 마감 기준 상위 3종.

- **VW-2601** 132장 판매, 판매율 **47.1%**, 잔여 148장
- VW-2603 168장, 40.0%, 잔여 252장
- VW-2607 121장, 31.8%, 잔여 259장

VW-2603 은 M 사이즈가 오픈 사흘 만에 품절됐다. 리오더 1순위로 둔다.'
WHERE b.block_id=9059;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**문의 214건**

- 사이즈 41% (88건)
- 배송 27% (58건)
- 교환 18% (38건)
- 반품 9% (20건)

**반품 38건, 12.2%, 2,190,000원.** 이 금액이 1차 정산 차감 기준이 된다.'
WHERE b.block_id=9061;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**반품 사유의 55% 가 사이즈 문제였다** (21건). 실측표와 실물이 최대 3cm 까지 차이 났다.

나머지는 단순 변심 9건, 색상 차이 5건, 배송 지연 3건.

2차 회차에 두 가지를 반영한다.

- 전 스타일 사이즈표를 실측해서 다시 쓴다
- 상세페이지에 모델이 입은 사이즈를 적는다'
WHERE b.block_id=9062;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='1차 판매 데이터에서 **판매율 상위 5스타일**을 골라 리오더한다.

사업성 검토 때 60% 로 줄였던 분량을 이번 회차에서 회수한다.'
WHERE b.block_id=9063;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**1,800장, 39,600,000원.** 발주 03-18, 납기 04-01~03.

- 오버핏 블레이저 320장, 12,160,000원
- 케이블 니트 480장, 9,360,000원
- 와이드 슬랙스 420장, 6,510,000원
- 그 외 2종'
WHERE b.block_id=9065;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**C공장은 뺐다.** 1차 때 이틀 지연된 게 공장 귀책으로 확인됐다.

A공장에 니트 3종, B공장에 우븐 2종으로 두 곳만 쓴다. 아우터는 이번 회차에서 제외했다.'
WHERE b.block_id=9067;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**04-03 정시 입고.**

발주 1,800 / 입고 1,796 / 차이 −4 (불량 폐기)

합격률 **99.8%.** 1차 99.3% 보다 올라갔다.'
WHERE b.block_id=9069;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**3월 마감**
GMV **91,200,000원.** 시즌 들어 가장 높은 달이다.
주문 1,289건, 객단가 70,750원, 누적 판매율 54.2%

**4월 진행분**
04-08 기준 GMV 18,600,000원. 마감 집계는 05-01 에 나온다.'
WHERE b.block_id=9070;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**기획전**
4월 봄 기획전을 04-06 에 신청했다. 선정 결과는 **04-12 에 나온다.**

**재입고 알림**
리오더 5종에 대해 알림을 보냈다. 관심 고객 **412명** (04-04).

**쿠폰**
봄맞이 15%, 브랜드가 절반 부담.'
WHERE b.block_id=9072;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='리오더한 5종 중 **3종이 다시 품절됐다.**

VW-2601, VW-2603, VW-2607.

재입고하고 평균 일주일 만에 다 나갔다. 3차 발주를 검토해야 한다.'
WHERE b.block_id=9073;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**3월 마감**
반품 34건, **9.4%**, 3,420,000원.

사이즈 때문에 들어온 반품이 **21건에서 8건으로** 줄었다. 1차 때 잡은 개선 과제 두 가지를 반영한 결과로 본다. 전체 반품율도 12.2% 에서 9.4% 로 내려갔다.'
WHERE b.block_id=9075;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='2026-04-08 기준.

- 기획전 선정 결과 04-12 예정
- 4월 마감 집계 05-01
- 2차 정산 입금 예정 04-10 (조은비)
- 3차 발주 여부는 4월 마감 보고 판단'
WHERE b.block_id=9076;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**정산 주기**
당월 1일부터 말일까지 구매확정된 건을 익월 10일에 받는다. 구매확정은 배송완료 후 7일이 지나면 자동으로 잡힌다.

**세금계산서**
정산액 기준으로 매출 계산서 1건을 발행한다. 계산서 총액과 입금액이 같아야 한다.'
WHERE b.block_id=9087;

UPDATE `text` t JOIN block b ON b.type_id=t.txt_id AND b.type='TEXT'
SET t.content='**1차 (2026-02월분)**

- 총매출 68,400,000원, 정산서와 일치
- 수수료 10,260,000원, 정산서와 일치
- 반품 차감은 우리 집계 2,116,000원인데 정산서는 2,190,000원. **74,000원 차이.**

확인해보니 반품 2건이 중복으로 차감돼 있었다. 03-07 에 이의를 넣어 **03-09 에 무신사가 인정**했고, 2차 정산에 가산해서 받았다.

**2차 (2026-03월분)**

이의분 74,000원이 가산된 걸 확인했고 그 외 차이는 없다. 대조 04-03, 계산서 수취 04-05, 입금 예정 04-10.'
WHERE b.block_id=9092;

-- =====================================================================
-- 5. 체크리스트 — 스테이지 접두사 잔재
-- =====================================================================
UPDATE checklist SET content='입점 세팅 착수' WHERE content='S3 온보딩 착수';

-- =====================================================================
-- 6. 이미지 캡션 — 줄표 제거
-- =====================================================================
UPDATE image SET caption='무신사 우먼 아우터 랭킹 (2025-11-10 캡처)'        WHERE img_id=9001;
UPDATE image SET caption='니트 카테고리 월간 방문자 추이. 11월 들어 늘었다' WHERE img_id=9022;
UPDATE image SET caption='경쟁 브랜드 가격 분포. 우리 가격대인 6~12만원 구간이 얇다' WHERE img_id=9023;
UPDATE image SET caption='26SS 룩북 01, 모델 착용 (M)'                      WHERE img_id=9006;
UPDATE image SET caption='26SS 룩북 02, 모델 착용 (S)'                      WHERE img_id=9007;
UPDATE image SET caption='오버핏 블레이저 디테일 (버튼과 안감)'             WHERE img_id=9025;
UPDATE image SET caption='크루넥 니트 3컬러, 컬러칩용'                      WHERE img_id=9026;
UPDATE image SET caption='룩북 3번 신. 크롭하면 상품이 잘려서 다시 찍었다'  WHERE img_id=9027;
UPDATE image SET caption='룩북 4번 신, 재촬영본'                            WHERE img_id=9028;
UPDATE image SET caption='룩북 7번 신, 재촬영본'                            WHERE img_id=9029;
UPDATE image SET caption='반려된 컷. 900x900 으로 규격 미달'                WHERE img_id=9008;
UPDATE image SET caption='1200x1200 으로 재작업한 컷'                       WHERE img_id=9009;
UPDATE image SET caption='제출 완료 화면. 접수번호 MS-25-11842'             WHERE img_id=9010;
UPDATE image SET caption='반려된 컷. 배경이 순백이 아니다'                  WHERE img_id=9030;
UPDATE image SET caption='누끼 따서 배경 순백으로 바꾼 컷'                  WHERE img_id=9031;
UPDATE image SET caption='사이즈표 단위를 cm 로 통일한 뒤'                  WHERE img_id=9032;
UPDATE image SET caption='입점 승인 통보 (2025-12-16)'                      WHERE img_id=9011;
UPDATE image SET caption='MD 피드백 메일 원문, 3개 항목'                    WHERE img_id=9033;
UPDATE image SET caption='파트너센터 첫 진입 화면. 입점 상태 승인'          WHERE img_id=9034;
UPDATE image SET caption='2차 시안. 모델컷을 바꿔 실제 색상과 맞췄다'       WHERE img_id=9035;
UPDATE image SET caption='컬러칩 이미지. 색상 오인 문의를 줄이려고 넣었다'  WHERE img_id=9036;
UPDATE image SET caption='상세페이지 사이즈 섹션. 실측 기준을 적었다'       WHERE img_id=9037;
UPDATE image SET caption='오픈 당일 상품 목록. 12스타일 노출 확인'          WHERE img_id=9038;
UPDATE image SET caption='2월 매출 대시보드'                                WHERE img_id=9014;
UPDATE image SET caption='클레임 접수. 봉제 터짐 (VW-2602)'                 WHERE img_id=9015;
UPDATE image SET caption='클레임 접수. 사이즈 편차 (VW-2603)'               WHERE img_id=9016;
UPDATE image SET caption='오버핏 블레이저가 카테고리 랭킹 41위에 들었다'    WHERE img_id=9039;
UPDATE image SET caption='유입 경로. 검색 62%, 기획전 24%, 기타 14%'        WHERE img_id=9040;
UPDATE image SET caption='재고 20% 아래로 떨어진 4개 SKU. 2차 발주 우선 대상' WHERE img_id=9041;
UPDATE image SET caption='3월 매출 대시보드. 시즌 최고 매출'                WHERE img_id=9017;
UPDATE image SET caption='재입고 알림 발송, 412명'                          WHERE img_id=9018;
UPDATE image SET caption='리뷰 평점 추이. 4.5 에서 4.2 로 떨어졌다'         WHERE img_id=9042;
UPDATE image SET caption='3월 반품 사유. 사이즈 52%, 색상 21%, 변심 18%'    WHERE img_id=9043;
UPDATE image SET caption='봄 아우터 야외 재촬영본. 교체 작업 중'            WHERE img_id=9044;
UPDATE image SET caption='1차 입금 내역 (2026-03-10, 55,950,000원)'         WHERE img_id=9019;
UPDATE image SET caption='홈택스 조회, 1차 계산서'                          WHERE img_id=9020;
UPDATE image SET caption='홈택스 조회, 2차 계산서 (2026-04-05)'             WHERE img_id=9021;
UPDATE image SET caption='1차 정산 반품 차감 74,000원 차이. 이의 제기 근거' WHERE img_id=9045;
UPDATE image SET caption='기획전 판매분에 수수료 18% 가 붙은 화면. 별도 약관 확인 후 정상 처리' WHERE img_id=9046;
UPDATE image SET caption='홈택스 3월분 세금계산서 발행 완료 (04-10)'        WHERE img_id=9047;

-- =====================================================================
-- 7. 파일 버전 코멘트 — 줄표 제거 · 빈 값 채우기
-- =====================================================================
UPDATE file_version SET comment='상신본. 11-29 반려됨'                       WHERE file_version_id=9005;
UPDATE file_version SET comment='반품율 12% 반영해 재상신. 최종 승인본'      WHERE file_version_id=9006;
UPDATE file_version SET comment='파트너센터 세팅 끝내고 받은 확인서'         WHERE file_version_id=9012;
UPDATE file_version SET comment='원산지 코드 오류 수정 (VNM → VN)'           WHERE file_version_id=9014;
UPDATE file_version SET comment='118건 전부 통과. 최종본'                    WHERE file_version_id=9018;
UPDATE file_version SET comment='A공장 1차 입고분'                           WHERE file_version_id=9021;
UPDATE file_version SET comment='3월분. 1차 이의분 74,000원 가산 반영'       WHERE file_version_id=9025;
UPDATE file_version SET comment='대조 결과 74,000원 차이 발견'               WHERE file_version_id=9026;
UPDATE file_version SET comment='출원증. 등록증은 2026-03 에 나온다'         WHERE file_version_id=9030;
UPDATE file_version SET comment='양측 서명 완료본. 승인 후 올렸다'           WHERE file_version_id=9028;
UPDATE file_version SET comment='물류비 산정 기준 확인해서 반영. 실부담 17.2%' WHERE file_version_id=9031;
UPDATE file_version SET comment='디자인 시안 적용. 텍스트 위주 초안에서 이미지 레이아웃으로' WHERE file_version_id=9032;
UPDATE file_version SET comment='브랜드 히스토리 2p 추가. MD 가 스토리가 부족하다고 했다' WHERE file_version_id=9033;
UPDATE file_version SET comment='정방형으로 다시 편집하고 재촬영 3컷 교체'   WHERE file_version_id=9034;
UPDATE file_version SET comment='수수료 12% 를 15% 로 정정 (계약 조건 확인)' WHERE file_version_id=9035;
UPDATE file_version SET comment='스타일별 차등 마진 확정본. 아우터 올리고 기본티 내렸다' WHERE file_version_id=9036;
UPDATE file_version SET comment='니트 4종 원단 혼용률 표기 추가'             WHERE file_version_id=9037;
UPDATE file_version SET comment='스타일별 수량 재배분 반영. 총 수량은 그대로' WHERE file_version_id=9038;
UPDATE file_version SET comment='니트 3종 원단이 단종돼 대체 원단 스펙으로 교체' WHERE file_version_id=9039;
UPDATE file_version SET comment='대체 원단 색차 허용 범위 명시 (ΔE 1.5 이내)' WHERE file_version_id=9040;
UPDATE file_version SET comment='inch 로 적힌 5건을 cm 로 통일'              WHERE file_version_id=9042;
UPDATE file_version SET comment='샘플 실측값으로 다시 씀. 패턴 스펙과 최대 1.5cm 차이가 있었다' WHERE file_version_id=9043;
UPDATE file_version SET comment='통화 내용 받아적은 초안'                    WHERE file_version_id=9044;
UPDATE file_version SET comment='항목별로 대응 방침, 담당자, 기한을 붙였다'  WHERE file_version_id=9045;
UPDATE file_version SET comment='소재와 세탁 표기 문구 통일, 과장 표현 3건 삭제' WHERE file_version_id=9048;
UPDATE file_version SET comment='전수 검품 결과. 불량 22장'                  WHERE file_version_id=9049;
UPDATE file_version SET comment='불량 사유 분류 추가 (봉제 14, 오염 5, 사이즈 3)' WHERE file_version_id=9050;
UPDATE file_version SET comment='전수 검품 결과. 불량률 1.8%'                WHERE file_version_id=9052;
UPDATE file_version SET comment='라인별 집계 추가. 신규 라인에 불량 68% 가 몰려 있다' WHERE file_version_id=9053;
UPDATE file_version SET comment='공장 회신과 재발방지 조치 첨부. 최종본'     WHERE file_version_id=9054;
UPDATE file_version SET comment='SKU 단위 대조. 기획전 판매분에 수수료 18% 항목이 보인다' WHERE file_version_id=9055;
UPDATE file_version SET comment='기획전 약관 확인 후 정상 처리. 차이 0원으로 종결' WHERE file_version_id=9056;

-- =====================================================================
-- 8. 이슈 제목 — 줄표 · 가운뎃점 정리
-- =====================================================================
UPDATE issue SET title='제품컷 12스타일 촬영과 보정'      WHERE issue_id=9001;
UPDATE issue SET title='불량 22장 판정하고 폐기'          WHERE issue_id=9006;
UPDATE issue SET title='AI 검토 수수료율 오차 확인'       WHERE issue_id=9017;
UPDATE issue SET title='반품율 12% 로 재산정'             WHERE issue_id=9018;
UPDATE issue SET title='상세페이지 시안 2차 수정 (모델컷 교체)' WHERE issue_id=9030;
UPDATE issue SET title='2차 발주 원단 수급 확인'          WHERE issue_id=9036;
UPDATE issue SET title='기여이익 기준 수익성 분석'        WHERE issue_id=9051;

-- =====================================================================
-- 9. 잔재 정리 (1차 적용 후 재검사에서 걸린 것)
--    이슈 본문이 "블록에 반영" 처럼 화면 구조를 언급하고 있었다.
--    실무자는 "개선 과제로 넘긴다" 라고 쓰지 "개선 액션 블록에 반영" 이라고 안 쓴다.
-- =====================================================================
UPDATE image SET caption='오버핏 블레이저 정면' WHERE img_id=9024;

UPDATE issue SET title='반품비 부담 주체 확인' WHERE issue_id=9026;

UPDATE issue SET content='AI 가 수수료를 12% 로 잡았는데 실제 계약 조건은 15% 다. 그대로 쓰면 마진이 3%p 부풀려진다. 확정본에는 사람이 계산한 수치로 바꿔 적었다.'
 WHERE issue_id=9017;

UPDATE issue SET content='① 사이즈 안 맞음 52% ② 색상 차이 21% ③ 단순 변심 18%. 앞 두 가지는 우리가 손댈 수 있는 부분이라 개선 과제로 넘긴다.'
 WHERE issue_id=9041;

UPDATE issue SET content='계약상 15% 인데 일부 기획전 판매분에 18% 가 붙어 있었다. 기획전에 참여하면 수수료가 다르다는 별도 약관이 있어서 정상이다. 헷갈리지 않게 정산 기준에 적어 뒀다.'
 WHERE issue_id=9049;
