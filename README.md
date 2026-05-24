# 크리에이터 정산 API

## 프로젝트 개요

온라인 강의 플랫폼에서 크리에이터(강사)의 월별 수익을 정산하는 백엔드 API입니다.
판매 내역과 취소 내역을 기반으로 수수료를 계산하고, 정산 상태(PENDING → CONFIRMED → PAID)를 관리합니다.

---

## 기술 스택

| 항목 | 선택 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.14 |
| ORM | Spring Data JPA |
| Database | H2 in-memory |
| Build Tool | Gradle |

> H2 in-memory DB를 사용하므로 별도 DB 설치 없이 바로 실행 가능합니다.

---

## 실행 방법

```bash
# 1. 프로젝트 클론
git clone https://github.com/jmjmff/creator-settlement.git
cd creator-settlement

# 2. 빌드 및 실행
./gradlew bootRun
```

서버 실행 후 `http://localhost:8080` 에서 API 사용 가능합니다.

**H2 콘솔 (DB 직접 조회)**
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa` / Password: (빈칸)

> 앱 실행 시 `data.sql`의 샘플 데이터가 자동으로 삽입됩니다.

---

## 구현 범위

### 필수 구현
- 판매 내역 등록
- 취소 내역 등록
- 크리에이터별 월별 정산 조회
- 운영자 기간별 정산 집계
- H2 기반 샘플 데이터 자동 삽입

### 선택 구현
- 정산 상태 관리: PENDING → CONFIRMED → PAID
- 동일 기간 중복 정산 방지
- 정산 내역 CSV 다운로드
- 수수료율 변경 이력 관리 (과거 정산은 당시 수수료율 적용)

---

## 요구사항 해석 및 가정

### 정산 기간 기준
- **판매**: `payment_at` (결제 완료 일시) 기준
- **취소**: `cancelled_at` (취소 일시) 기준

월 경계는 해당 월 전체를 대상으로 합니다.
경계값 누락을 방지하기 위해 시작일은 포함하고, 다음 달 1일 00:00:00은 제외하는 방식으로 처리했습니다.

```
예: 2025-03 정산
- 시작: 2025-03-01 00:00:00 이상
- 종료: 2025-04-01 00:00:00 미만
```

예를 들어 1월에 결제하고 2월에 취소한 건은 "1월 판매" + "2월 취소"로 각각 다른 월에 집계됩니다.

### 수수료 계산
```
순매출 = 총매출 - 총환불
수수료 = 순매출 × 수수료율 (HALF_UP 반올림)
정산액 = 순매출 - 수수료
```

### 빈 월 조회
판매 데이터가 없는 월을 조회하면 모든 금액을 0으로 응답합니다 (에러 아님).

### 음수 정산
판매보다 환불이 많으면 정산액이 음수가 될 수 있으며, 이는 정상 동작입니다.

---

## 설계 결정과 이유

### 1. 수수료율 스냅샷 저장
정산 생성 시점의 수수료율을 `Settlement.commissionRate`에 저장합니다.
이후 수수료율이 변경되어도 과거 정산 금액이 변하지 않아 정산의 불변성이 보장됩니다.

### 2. 수수료율 이력 관리 (CommissionRateHistory)
```
rate    appliedFrom    appliedTo
0.15    2024-01-01     2024-12-31
0.20    2025-01-01     null         ← 현재 적용 중
```
`appliedTo`가 null이면 현재까지 유효한 수수료율을 의미합니다.
정산 기준월 1일 기준으로 해당 수수료율을 자동으로 조회하여 적용합니다.

### 3. 중복 정산 방지 이중 방어
- **1차 (소프트 체크)**: 서비스 레이어에서 `existsByCreatorIdAndYearMonth()` 조회 → 409 반환
- **2차 (하드 체크)**: DB의 `(creator_id, year_month) UNIQUE` 제약 → 동시 요청도 차단

### 4. 정산 상태 전이
```
PENDING → CONFIRMED → PAID
```
잘못된 순서의 상태 전이(예: PENDING → PAID)는 422 예외를 반환합니다.

---

## 데이터 모델 설명

### ERD 구조

```
Creator (크리에이터)
  └── Course (강의) N:1 → Creator
        └── SaleRecord (판매 내역) N:1 → Course
              └── CancelRecord (취소 내역) 1:1 → SaleRecord
  └── Settlement (정산) N:1 → Creator
CommissionRateHistory (수수료율 이력, 독립 테이블)
```

### 테이블 명세

**creators**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | PK |
| name | VARCHAR | 크리에이터 이름 |
| email | VARCHAR | 이메일 |

**courses**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | PK |
| title | VARCHAR | 강의 제목 |
| creator_id | BIGINT | FK → creators |

**sale_records**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | PK |
| course_id | BIGINT | FK → courses |
| student_id | VARCHAR | 수강생 ID |
| payment_amount | DECIMAL | 결제 금액 |
| payment_at | DATETIME | 결제 일시 (정산 기준) |
| cancelled | BOOLEAN | 취소 여부 |

**cancel_records**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | PK |
| sale_record_id | BIGINT | FK → sale_records (1:1) |
| refund_amount | DECIMAL | 환불 금액 |
| cancelled_at | DATETIME | 취소 일시 (정산 기준) |

**settlements**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | PK |
| creator_id | BIGINT | FK → creators |
| year_month | VARCHAR | 정산 월 (예: 2025-03) |
| status | VARCHAR | PENDING / CONFIRMED / PAID |
| commission_rate | DECIMAL | 정산 시점 수수료율 스냅샷 |
| total_sales_amount | DECIMAL | 총 판매 금액 |
| total_refund_amount | DECIMAL | 총 환불 금액 |
| net_sales_amount | DECIMAL | 순 판매 금액 |
| commission | DECIMAL | 수수료 |
| settlement_amount | DECIMAL | 최종 정산액 |
| sale_count | INT | 판매 건수 |
| cancel_count | INT | 취소 건수 |
| confirmed_at | DATETIME | 확정 일시 |
| paid_at | DATETIME | 지급 일시 |

**commission_rate_histories**

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | BIGINT | PK |
| rate | DECIMAL | 수수료율 (예: 0.15, 0.20) |
| applied_from | DATE | 적용 시작일 |
| applied_to | DATE | 적용 종료일 (null = 현재 유효) |

---

## API 목록 및 예시

### 판매 내역

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/sale-records` | 판매 등록 |
| POST | `/api/cancel-records` | 취소 등록 |
| GET | `/api/sale-records?creatorId=1&startDate=2025-03-01&endDate=2025-03-31` | 판매 목록 조회 |

**판매 등록 요청**
```json
{
  "courseId": 1,
  "studentId": "student-1",
  "paymentAmount": 50000,
  "paymentAt": "2025-03-05T10:00:00"
}
```

**판매 등록 응답**
```json
{
  "id": 1,
  "courseId": 1,
  "courseTitle": "강의1",
  "creatorId": 1,
  "creatorName": "김강사",
  "studentId": "student-1",
  "paymentAmount": 50000,
  "paymentAt": "2025-03-05T10:00:00",
  "cancelled": false,
  "cancelInfo": null
}
```

**취소 등록 요청**
```json
{
  "saleRecordId": 1,
  "refundAmount": 50000,
  "cancelledAt": "2025-03-10T10:00:00"
}
```

---

### 정산

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/settlements/creator/{creatorId}?yearMonth=2025-03` | 크리에이터별 월 정산 조회 (실시간) |
| GET | `/api/settlements/admin?startDate=2025-03-01&endDate=2025-03-31` | 운영자 전체 집계 (실시간) |
| POST | `/api/settlements?creatorId=1&yearMonth=2025-03` | 정산 생성 (PENDING) |
| GET | `/api/settlements/{id}` | 정산 단건 조회 |
| PATCH | `/api/settlements/{id}/confirm` | 정산 확정 (PENDING → CONFIRMED) |
| PATCH | `/api/settlements/{id}/pay` | 정산 지급 (CONFIRMED → PAID) |
| GET | `/api/settlements/admin/csv` | 전체 정산 CSV 다운로드 |

**크리에이터별 월 정산 조회 응답**
```json
{
  "creatorId": 1,
  "creatorName": "김강사",
  "yearMonth": "2025-03",
  "totalSalesAmount": 260000,
  "totalRefundAmount": 110000,
  "netSalesAmount": 150000,
  "commission": 30000,
  "settlementAmount": 120000,
  "saleCount": 4,
  "cancelCount": 2
}
```

**정산 생성 응답**
```json
{
  "id": 1,
  "creatorId": 1,
  "creatorName": "김강사",
  "yearMonth": "2025-03",
  "status": "PENDING",
  "commissionRate": 0.2000,
  "totalSalesAmount": 260000.00,
  "totalRefundAmount": 110000.00,
  "netSalesAmount": 150000.00,
  "commission": 30000.00,
  "settlementAmount": 120000.00,
  "saleCount": 4,
  "cancelCount": 2,
  "confirmedAt": null,
  "paidAt": null
}
```

---

## 에러 응답 형식

```json
{
  "status": 400,
  "message": "에러 메시지"
}
```

| 상태코드 | 발생 상황 | 예시 메시지 |
|---|---|---|
| 400 | 유효성 검사 실패, 날짜 형식 오류 | `paymentAmount: 결제 금액은 0보다 커야 합니다` |
| 404 | 크리에이터 / 판매 내역 없음 | `크리에이터를 찾을 수 없습니다. id=999` |
| 409 | 이미 취소된 판매 재취소, 중복 정산 생성 | `이미 취소된 판매 내역입니다. id=3` |
| 422 | 잘못된 상태 전이 | `PENDING 상태에서만 확정할 수 있습니다. 현재 상태: PAID` |

---

## 테스트 시나리오 및 검증 케이스

### data.sql에 포함된 테스트 케이스

| 케이스 | 대상 | 조회 월 | 핵심 검증 포인트 |
|---|---|---|---|
| 동일 월 다수 취소 | creator-1 | 2025-04 | 취소 3건 누적 합산 → 정산액 0 |
| 전액 환불 → 정산 0원 | creator-3 | 2025-05 | 순매출 0 → 수수료 0 |
| 월 경계 (1월 말 결제 → 2월 취소) | creator-2 | 2025-01 / 02 | 결제월과 취소월 분리 집계 |
| 월 경계 (2월 말 결제 → 3월 취소) | creator-2 | 2025-02 / 03 | 결제월과 취소월 분리 집계 |
| 동일 수강생 중복 구매 | creator-1 | 2025-06 | 동일 수강생 2건 합산 처리 |
| 음수 정산 | creator-1 | 2025-09 | 판매 없이 취소만 발생 → 정산액 음수 |
| 수수료 반올림 (HALF_UP) | creator-3 | 2025-07 | 33,333 × 0.20 = 6,666.6 → 6,667 |
| 다건 합산 | creator-1 | 2025-07 | 4건 판매 합산 검증 |

### 추가한 케이스와 이유

**1. 정산 상태 전이 (PENDING → CONFIRMED → PAID)**
정산 생성 후 confirm, pay 순서로 호출하여 상태가 올바르게 전이되는지 확인했습니다.
또한 PAID 상태에서 confirm을 재호출하여 잘못된 상태 전이가 차단되는지 검증했습니다.
→ 상태 전이 정상, `422 PENDING 상태에서만 확정할 수 있습니다. 현재 상태: PAID`

**2. 중복 정산 방지**
동일한 creatorId + yearMonth 조합으로 정산을 두 번 생성 요청하여 중복이 차단되는지 확인했습니다.
한 달에 정산이 두 번 나가면 안 되는 비즈니스 규칙을 검증하는 케이스입니다.
→ `409 이미 정산이 생성된 기간입니다. creatorId=1, yearMonth=2025-06`

**3. CSV 다운로드**
전체 정산 내역이 CSV 파일로 정상 다운로드되는지, Excel에서 한글이 깨지지 않는지 확인했습니다.
UTF-8 BOM을 포함하여 Excel 호환성을 확보했습니다.
→ 정상 다운로드, 한글 깨짐 없음

**4. 수수료율 이력 15% 검증 (sale-23, 2024-06)**
2024년(15%)과 2025년(20%) 수수료율이 각각 올바르게 적용되는지 확인했습니다.
과거 정산이 이후 수수료율 변경에 영향받지 않음을 검증하는 핵심 케이스입니다.
→ `commissionRate: 0.15`, `commission: 15000` 정상 적용

**5. 이미 취소된 판매 재취소 시도**
이중 취소 발생 시 환불이 두 번 나가는 심각한 문제가 생길 수 있어 서버가 차단하는지 확인했습니다.
→ `409 이미 취소된 판매 내역입니다.`

**6. 결제 금액 음수 입력**
유효성 검사가 올바르게 동작하는지 확인했습니다.
→ `400 paymentAmount: 결제 금액은 0보다 커야 합니다`

**7. 존재하지 않는 크리에이터 조회**
→ `404 크리에이터를 찾을 수 없습니다. id=999`

**8. 잘못된 연월 형식 입력 (버그 수정)**
`2025-3`처럼 잘못된 형식 입력 시 500 오류가 발생하는 버그를 발견하고 수정했습니다.
`DateTimeParseException`을 `GlobalExceptionHandler`에서 처리하여 400으로 응답하도록 개선했습니다.
→ `400 날짜 형식이 올바르지 않습니다. 올바른 형식: yyyy-MM`

---

## 테스트 실행 방법

```bash
# 전체 테스트 실행
./gradlew test
```

### Postman으로 API 테스트

```
① 정산 생성
   POST http://localhost:8080/api/settlements?creatorId=1&yearMonth=2025-03

② 확정
   PATCH http://localhost:8080/api/settlements/1/confirm

③ 지급
   PATCH http://localhost:8080/api/settlements/1/pay

④ 중복 방지 확인 (409 떠야 정상)
   POST http://localhost:8080/api/settlements?creatorId=1&yearMonth=2025-03

⑤ CSV 다운로드
   GET http://localhost:8080/api/settlements/admin/csv
```

---

## 미구현 / 제약사항

- **인증/인가 미구현**: `creatorId`를 쿼리 파라미터로 전달하는 방식으로 대체
- **데이터 초기화**: H2 인메모리 DB 사용으로 서버 재시작 시 데이터가 초기화됨

---

## AI 활용 범위

- **활용 도구**: Claude (claude.ai)
- **활용 내용**:
  - 프로젝트 초기 구조 설계 및 엔티티 설계 가이드
  - 정산 계산 로직 코드 초안 생성
  - 테스트 케이스 시나리오 제안
  - 예외 처리 구조 설계 조언
- **본인 검증**:
  - 생성된 코드를 직접 실행하여 각 API 응답 확인
  - Postman으로 모든 엔드포인트 직접 테스트
  - 정산 금액 수동 계산으로 응답값 검증
  - 상태 전이 오류 케이스 직접 확인

> AI가 제안한 코드를 그대로 사용하지 않고, 실행 결과를 직접 확인하고 수정하는 과정을 거쳤습니다.
