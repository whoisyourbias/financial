# 프로젝트 07 — 결제 상태 머신

| 항목           | 값                                               |
| -------------- | ------------------------------------------------ |
| 기간           | 2026-11-23 ~ 2026-12-06                          |
| 상태           | PLANNED                                          |
| 선행 프로젝트  | 01~03, 05                                        |
| 목표 태그      | `p07-payment-lifecycle`                          |
| 주력 채용 신호 | 결제 승인·매입·취소·환불, 상태 머신, 원장 정합성 |

## 문제 정의

결제는 하나의 성공 boolean이 아니라 승인, 매입, 취소, 환불이 시간과 경쟁 조건 속에서 전이하는 과정입니다. 취소와 매입이 동시에 요청되거나 부분 환불 합계가 매입 금액을 넘으면 금전 기록과 상태가 어긋날 수 있습니다. 이 프로젝트는 허용된 상태 전이를 하나의 명세로 만들고 모든 금전 변화가 원장의 상쇄 posting으로 설명되게 합니다.

외부 PG는 결정론적 stub으로 대체합니다. 실제 PG 연동 또는 상용 결제 처리를 주장하지 않습니다.

## 필수 범위

- payment aggregate와 명시적 상태 전이
- `CREATED → AUTHORIZED → CAPTURED`
- 승인 전 취소, 승인 취소, 매입 뒤 부분·전체 환불
- idempotent authorize·capture·cancel·refund command
- capture와 cancel의 동시 요청 처리
- 누적 환불 금액이 capture 금액을 넘지 않는 제약
- 원장 posting과 payment operation 연결
- 기존 posting 삭제 없이 reversal·refund posting 생성
- 상태 전이 history와 actor·reason·correlation
- 결정론적 partner stub

## 비범위

- 실제 카드번호·PG·카드사 연동
- 할부, 복합결제, 포인트, 세금계산서
- chargeback·분쟁
- 여러 통화 결제
- 서비스 분리
- 결제 성공률을 실제 사업 성과로 표현

## 상태 전이

| 현재 상태          | 명령                | 다음 상태                        | 금전 기록                          |
| ------------------ | ------------------- | -------------------------------- | ---------------------------------- |
| CREATED            | authorize           | AUTHORIZED                       | 승인 hold 기록 또는 별도 승인 기록 |
| CREATED            | cancel              | CANCELED                         | 금전 posting 없음                  |
| AUTHORIZED         | capture             | CAPTURED                         | 매입 posting                       |
| AUTHORIZED         | cancel              | CANCELED                         | 승인 해제 기록                     |
| CAPTURED           | partial refund      | PARTIALLY_REFUNDED               | 환불 posting                       |
| CAPTURED           | full refund         | REFUNDED                         | 전체 환불 posting                  |
| PARTIALLY_REFUNDED | partial/full refund | PARTIALLY_REFUNDED 또는 REFUNDED | 추가 환불 posting                  |

표에 없는 전이는 명시적으로 거절합니다. 구현 상태와 표가 다르면 RELEASED할 수 없습니다.

## 결제 불변식

1. capture 금액은 authorize 금액을 넘지 않습니다.
2. 누적 refund 금액은 capture 금액을 넘지 않습니다.
3. 같은 command key는 한 payment operation 결과만 만듭니다.
4. cancel과 capture가 동시에 모두 성공하지 않습니다.
5. payment state와 operation history가 같은 transaction에서 확정됩니다.
6. 원장의 capture·refund 순액과 payment 순매입 금액이 같습니다.
7. 상태 변경은 history를 삭제하지 않습니다.

## 설계 후보

### 상태 전이 표현

- 분산된 조건문
- 명시적 transition table과 domain method
- 외부 state machine framework

명시적 transition table과 domain method를 기준 후보로 사용합니다. 외부 framework는 전이 수와 운영 복잡도에 비해 필요한지 검토하며, 사용하지 않으면 스택에 넣지 않습니다.

### 원장 반영 시점

- payment transaction에서 ledger port 동기 호출
- operation 확정 뒤 event로 ledger 반영

프로젝트 07은 단일 프로세스이므로 동기 port를 기준으로 원자성을 확보합니다. 프로젝트 09에서 서비스 분리 시 이 가정을 다시 검토합니다.

## 계획 인터페이스

```text
POST /api/v1/payments
POST /api/v1/payments/{id}/authorizations
POST /api/v1/payments/{id}/captures
POST /api/v1/payments/{id}/cancellations
POST /api/v1/payments/{id}/refunds
GET  /api/v1/payments/{id}
GET  /api/v1/payments/{id}/operations
```

모든 변경 요청은 `Idempotency-Key`를 요구합니다. refund는 금액과 reason을 포함하며 서버가 누적 환불 한도를 검사합니다.

## 데이터 흐름

```text
payment command
  → idempotency·authorization
  → 현재 state·version 확인
  → transition validation
  → deterministic partner stub
  → ledger posting
  → payment state + operation history + outbox commit
```

## 산출물

- 상태 전이표와 payment aggregate
- operation·idempotency model
- partner stub과 failure fixture
- capture·refund 원장 매핑
- 동시 전이·부분 환불 테스트
- 상태·원장 대사 query
- state machine·ledger boundary ADR

## 10일 작업 계획

| 일차 | 작업                      | 완료 기준                       |
| ---- | ------------------------- | ------------------------------- |
| 1    | 결제 용어·전이·불변식     | 전이표와 비범위 확정            |
| 2    | 상태 표현·원장 경계 ADR   | 현재 단일 transaction 가정 명시 |
| 3    | payment·operation schema  | history와 idempotency 제약      |
| 4    | create·authorize·cancel   | 허용·거절 전이 테스트           |
| 5    | capture와 원장 posting    | payment·ledger 원자적 결과      |
| 6    | partial·full refund       | 누적 한도와 reversal posting    |
| 7    | 동시 전이·query·history   | cancel/capture 경쟁 방어        |
| 8    | 실패·중복·정합성 실험     | 상태·operation·원장 대조        |
| 9    | ADR·diagram·evidence      | 전이표와 코드 mapping           |
| 10   | Portfolio Review·Red Team | PASS와 🔴 0건 또는 HOLD         |

## 테스트와 측정

- 표의 모든 허용·불허 전이
- authorize·capture·cancel·refund 중복 요청
- capture와 cancel 동시 실행
- 두 partial refund 동시 실행
- capture 초과, 누적 refund 초과
- partner timeout 전·후 transaction 결과
- ledger posting 실패와 payment rollback
- state·operation·journal 합계 대조

대표 결과는 처리량보다 상태·금전 정합성입니다. 동시성 실험의 요청 수와 환경은 공개하지만 실제 결제 규모로 표현하지 않습니다.

## 필요한 증거

- 상태 전이표와 domain test mapping
- operation·payment·journal query 대조
- 동시 cancel/capture 결과와 lock/version 근거
- 누적 환불 합계와 원장 순액
- partner failure fixture와 rollback 결과
- idempotency key당 고유 operation 수
- 미지원 결제 기능과 실제 PG 미연동 limitation

## 예상 면접 질문

1. 승인과 매입을 분리한 이유는 무엇인가요?
2. capture와 cancel이 동시에 요청되면 어느 전이가 승리하며 왜 그렇습니까?
3. 부분 환불을 기존 posting 수정이 아니라 새 posting으로 남긴 이유는 무엇인가요?
4. payment와 ledger가 서비스로 분리되면 현재 transaction 가정이 어떻게 깨집니까?
5. 상태 전이표와 코드가 어긋나지 않게 어떻게 검증합니까?

## 주요 위험과 다음 회차 연결

- 카드 결제 전체를 구현했다고 표현하지 않습니다.
- authorize hold와 실제 자금 이동을 모호하게 섞지 않습니다.
- operation history를 event sourcing이라고 부르지 않습니다.
- 프로젝트 08은 실제 외부 파트너가 보이는 지연·중복 webhook·정산 경계를 stub으로 확장합니다.
