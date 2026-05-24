-- =====================================================================
-- 수수료율 이력 (commission_rate_histories)
-- appliedTo = null → 현재까지 유효한 최신 수수료율
-- 정산 생성 시 해당 월 1일 기준으로 적용 수수료율을 스냅샷으로 저장함
-- =====================================================================
INSERT INTO commission_rate_histories (rate, applied_from, applied_to)
    VALUES (0.1500, '2024-01-01', '2024-12-31');  -- 2024년: 15%
INSERT INTO commission_rate_histories (rate, applied_from, applied_to)
    VALUES (0.2000, '2025-01-01', NULL);           -- 2025년~현재: 20% (기본값과 동일)

-- 크리에이터
INSERT INTO creators (name, email) VALUES ('김강사', 'kim@example.com');
INSERT INTO creators (name, email) VALUES ('이강사', 'lee@example.com');
INSERT INTO creators (name, email) VALUES ('박강사', 'park@example.com');

-- 강의 (course-1, course-2 → creator-1 / course-3 → creator-2 / course-4 → creator-3)
INSERT INTO courses (title, creator_id) VALUES ('강의1', 1);
INSERT INTO courses (title, creator_id) VALUES ('강의2', 1);
INSERT INTO courses (title, creator_id) VALUES ('강의3', 2);
INSERT INTO courses (title, creator_id) VALUES ('강의4', 3);

-- =====================================================================
-- 기본 판매/취소 데이터 (sale-1 ~ sale-7, cancel-1 ~ cancel-3)
-- =====================================================================

-- creator-1 소속 강의 판매 (2025-03)
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (1, 'student-1', 50000, '2025-03-05 10:00:00', false);   -- sale-1
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (1, 'student-2', 50000, '2025-03-15 14:30:00', false);   -- sale-2
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (2, 'student-3', 80000, '2025-03-20 09:00:00', true);    -- sale-3
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (2, 'student-4', 80000, '2025-03-22 11:00:00', true);    -- sale-4

-- creator-2 소속 강의 판매
-- sale-5: 1월 31일 23:30 KST → 1월 귀속 (월 경계 케이스)
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (3, 'student-5', 60000, '2025-01-31 23:30:00', true);    -- sale-5
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (3, 'student-6', 60000, '2025-03-10 16:00:00', false);   -- sale-6

-- creator-3 소속 강의 판매 (2025-02)
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (4, 'student-7', 120000, '2025-02-14 10:00:00', false);  -- sale-7

-- 취소 내역
-- cancel-1: sale-3 전액 환불 (2025-03-21)
INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (3, 80000, '2025-03-21 10:00:00');
-- cancel-2: sale-4 부분 환불 30000 (2025-03-23)
INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (4, 30000, '2025-03-23 10:00:00');
-- cancel-3: sale-5 환불 (2025-02-03) → 월 경계 케이스: 1월 판매, 2월 취소
INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (5, 60000, '2025-02-03 10:00:00');

-- =====================================================================
-- [케이스 1] 동일 월 다수 취소 (sale-8 ~ sale-10, cancel-4 ~ cancel-6)
-- creator-1이 2025-04에 취소 3건 발생 → 월 취소 집계 검증용
-- =====================================================================
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (1, 'student-8', 50000, '2025-04-03 10:00:00', true);    -- sale-8
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (1, 'student-9', 50000, '2025-04-10 13:00:00', true);    -- sale-9
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (2, 'student-10', 80000, '2025-04-18 09:30:00', true);   -- sale-10

INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (8, 50000, '2025-04-05 11:00:00');   -- cancel-4: sale-8 전액 환불
INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (9, 50000, '2025-04-12 15:00:00');   -- cancel-5: sale-9 전액 환불
INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (10, 80000, '2025-04-20 10:00:00');  -- cancel-6: sale-10 전액 환불

-- =====================================================================
-- [케이스 2] 전액 환불 후 해당 월 정산 0원 (sale-11, cancel-7)
-- creator-3의 2025-05: 판매 1건 전액 환불 → 순매출 0 → 정산액 0원
-- =====================================================================
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (4, 'student-11', 150000, '2025-05-10 10:00:00', true);  -- sale-11

INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (11, 150000, '2025-05-12 09:00:00'); -- cancel-7: sale-11 전액 환불 → 5월 정산 0원

-- =====================================================================
-- [케이스 3] 미래 날짜 판매 데이터 (sale-12 ~ sale-13)
-- 2025-12월 판매 → 미래 정산 미리보기 또는 조회 범위 경계 테스트용
-- =====================================================================
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (1, 'student-12', 50000, '2025-12-01 10:00:00', false);  -- sale-12: creator-1, 12월
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (3, 'student-13', 60000, '2025-12-15 14:00:00', false);  -- sale-13: creator-2, 12월

-- =====================================================================
-- [케이스 4] creator-2 월 경계 케이스 추가 (sale-14, cancel-8)
-- 2월 말(2025-02-28 23:50) 판매 → 2월 귀속, 3월 초 취소 → 취소는 3월 귀속
-- =====================================================================
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (3, 'student-14', 60000, '2025-02-28 23:50:00', true);   -- sale-14

INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (14, 60000, '2025-03-01 09:00:00');  -- cancel-8: 2월 말 판매, 3월 초 취소

-- =====================================================================
-- [케이스 5] 동일 수강생이 같은 강의를 두 번 구매 (sale-15 ~ sale-16)
-- student-15가 강의2(course-2, creator-1)를 2025-06에 두 번 결제
-- 중복 구매 허용 여부 및 정산 합산 검증용
-- =====================================================================
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (2, 'student-15', 80000, '2025-06-05 10:00:00', false);  -- sale-15: 첫 번째 구매
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (2, 'student-15', 80000, '2025-06-20 16:00:00', false);  -- sale-16: 두 번째 구매 (동일 수강생·강의)

-- =====================================================================
-- [케이스 6] 판매 없는 달에 취소만 발생 → 정산액 음수 (sale-17, cancel-9)
-- sale-17: 2025-08 판매 → cancel-9: 2025-09 취소 (다음 달 처리)
--
-- CancelRecord는 cancelledAt 기준으로 집계, SaleRecord는 paymentAt 기준으로 집계.
-- 이로 인해 달이 분리됨:
--   creator-1, 2025-08 정산: totalSales=100000, refund=0       → settlement= 80000
--   creator-1, 2025-09 정산: totalSales=0,      refund=100000  → net=-100000 → settlement=-80000 ← 음수!
-- =====================================================================
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (2, 'student-17', 100000, '2025-08-05 10:00:00', true);  -- sale-17: 8월 판매

INSERT INTO cancel_records (sale_record_id, refund_amount, cancelled_at)
    VALUES (17, 100000, '2025-09-02 10:00:00'); -- cancel-9: 9월 취소 → 9월 정산 음수 발생

-- =====================================================================
-- [케이스 7] 수수료 반올림(HALF_UP) 검증 (sale-18)
-- creator-3, 2025-07, 결제금액 33333원
--   commission = 33333 × 0.20 = 6666.6 → HALF_UP → 6667
--   settlement = 33333 - 6667 = 26666
-- 소수점 발생 시 반올림이 올바르게 적용되는지 확인
-- =====================================================================
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (4, 'student-18', 33333, '2025-07-10 11:00:00', false);  -- sale-18

-- =====================================================================
-- [케이스 8] 다건 정산 합산 검증 (sale-19 ~ sale-22)
-- creator-1, 2025-07, 100000원 × 4건, 취소 없음
--   totalSales=400000, commission=80000, settlement=320000
-- 깔끔한 숫자로 정산 집계 로직 수동 검증 용이
-- =====================================================================
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (1, 'student-19', 100000, '2025-07-03 09:00:00', false);  -- sale-19
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (1, 'student-20', 100000, '2025-07-10 10:00:00', false);  -- sale-20
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (2, 'student-21', 100000, '2025-07-17 14:00:00', false);  -- sale-21
INSERT INTO sale_records (course_id, student_id, payment_amount, payment_at, cancelled)
    VALUES (2, 'student-22', 100000, '2025-07-24 16:00:00', false);  -- sale-22

-- =====================================================================
-- CSV 다운로드 테스트용 정산 데이터 (settlements)
-- PAID 2건 / CONFIRMED 2건 / PENDING 2건 → 상태별 필터링 및 전체 출력 확인용
-- =====================================================================
INSERT INTO settlements (creator_id, year_month, commission_rate, total_sales_amount, total_refund_amount, net_sales_amount, commission, settlement_amount, sale_count, cancel_count, status, confirmed_at, paid_at)
VALUES
-- PAID (2건)
(1, '2025-03', 0.20, 260000, 110000, 150000, 30000, 120000, 4, 2, 'PAID',      '2025-04-01 10:00:00', '2025-04-05 10:00:00'),
(3, '2025-02', 0.20, 120000,      0, 120000, 24000,  96000, 1, 0, 'PAID',      '2025-03-01 10:00:00', '2025-03-05 10:00:00'),

-- CONFIRMED (2건)
(1, '2025-04', 0.20, 180000, 180000,      0,     0,      0, 3, 3, 'CONFIRMED', '2025-05-01 10:00:00', NULL),
(2, '2025-01', 0.20,  60000,      0,  60000, 12000,  48000, 1, 0, 'CONFIRMED', '2025-02-01 10:00:00', NULL),

-- PENDING (2건)
(1, '2025-07', 0.20, 400000,      0, 400000, 80000, 320000, 4, 0, 'PENDING',   NULL, NULL),
(3, '2025-07', 0.20,  33333,      0,  33333,  6667,  26666, 1, 0, 'PENDING',   NULL, NULL);