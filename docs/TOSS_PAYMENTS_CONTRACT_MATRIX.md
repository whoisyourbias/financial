# 토스페이먼츠 선정 계약 매트릭스

## 호환성 정의

선정한 공통 KRW 카드·간편결제·가상계좌 시나리오에서 공개 V2 계약의 경로, 헤더, 필수 입력, 응답 객체, 상태와 오류 의미를 계약 테스트로 고정합니다. 아직 구현되지 않은 모든 행의 상태는 `PLANNED`입니다.

실제 카드사·은행을 호출하지 않는 동작은 `/sandbox/**` 테스트 API가 결정론적으로 발생시킵니다. 이 namespace는 토스페이먼츠 공개 계약이 아닙니다.

## API 계약

| ID | 공개 표면 | 필수 규약 | sourceRefs | 계획 검증 | 상태 |
| --- | --- | --- | --- | --- | --- |
| TP-PAY-001 | `POST /v1/payments/confirm` | `paymentKey`, `orderId`, `amount` 검증 후 Payment 또는 Error 반환 | `toss-payments-core-api`, `toss-payments-payment-flow` | 정상·금액/주문 불일치·만료·중복 승인 | PLANNED |
| TP-PAY-002 | `GET /v1/payments/{paymentKey}` | 승인된 Payment를 고유 `paymentKey`로 조회 | `toss-payments-core-api` | 존재·부재·다른 MID 격리 | PLANNED |
| TP-PAY-003 | `GET /v1/payments/orders/{orderId}` | 승인된 Payment를 상점 주문번호로 조회 | `toss-payments-core-api` | 존재·부재·MID별 동일 주문번호 | PLANNED |
| TP-PAY-004 | `POST /v1/payments/{paymentKey}/cancel` | `cancelReason`, 선택 `cancelAmount`, 취소별 `transactionKey`, 전액·부분 취소 | `toss-payments-core-api`, `toss-payments-cancel-guide` | 전액·다회 부분·초과·동시·멱등 취소 | PLANNED |
| TP-VA-001 | `POST /v1/virtual-accounts` | 가상계좌 발급 뒤 입금 전 상태를 Payment로 표현 | `toss-payments-core-api`, `toss-payments-virtual-account` | 발급·중복 주문·잘못된 은행·만료 | PLANNED |

## 공통 wire 계약

| ID | 대상 | 규약 | sourceRefs | 계획 검증 | 상태 |
| --- | --- | --- | --- | --- | --- |
| TP-WIRE-001 | 인증 | 시크릿 키 기반 Basic 인증, MID·환경별 키 격리 | `toss-payments-api-keys`, `toss-payments-authorization` | 누락·형식 오류·잘못된 키·회전 grace | PLANNED |
| TP-WIRE-002 | 멱등성 | 문서가 지원한다고 명시한 POST에 `Idempotency-Key` 적용 | `toss-payments-authorization`, `toss-payments-idempotency` | 같은 키/본문, 같은 키/다른 본문, 처리 중 요청 | PLANNED |
| TP-WIRE-003 | 응답 | JSON 리소스 또는 `code`·`message` Error와 HTTP 상태 | `toss-payments-request-response`, `toss-payments-error-codes` | endpoint별 대표 4xx·5xx fixture | PLANNED |
| TP-WIRE-004 | 버전 | additive 변경과 파괴적 변경을 구분하고 구버전 소비자를 보호 | `toss-payments-versioning` | old/new contract 조합 | PLANNED |
| TP-WIRE-005 | Payment | 공통 필드와 method별 `card`, `easyPay`, `virtualAccount`, `cancels`, `failure` | `toss-payments-core-api`, `toss-payments-data-types`, `toss-payments-enum-codes` | schema snapshot과 JSON fixture | PLANNED |
| TP-WIRE-006 | 비범위 표기 | 세금·해외통화·에스크로 등은 지원 목록에서 제외하고 해당 요청의 Toss 호환 동작을 주장하지 않음 | `toss-payments-core-api` | 지원·비지원 fixture 목록과 샌드박스 자체 오류의 별도 표기 | PLANNED |

## 상태와 웹훅 계약

| ID | 대상 | 규약 | sourceRefs | 계획 검증 | 상태 |
| --- | --- | --- | --- | --- | --- |
| TP-STATE-001 | Payment 상태 | `READY`, `IN_PROGRESS`, `WAITING_FOR_DEPOSIT`, `DONE`, `CANCELED`, `PARTIAL_CANCELED`, `ABORTED`, `EXPIRED` | `toss-payments-core-api`, `toss-payments-enum-codes` | 결제수단별 허용·불허 전이 | PLANNED |
| TP-WEBHOOK-001 | `PAYMENT_STATUS_CHANGED` | Payment 상태 변경 payload를 MID별 endpoint로 전달 | `toss-payments-webhook-guide`, `toss-payments-webhook-events` | 정상·중복·늦은 전달 | PLANNED |
| TP-WEBHOOK-002 | `CANCEL_STATUS_CHANGED` | 취소 상태 변경을 Cancel payload로 전달 | `toss-payments-webhook-guide`, `toss-payments-webhook-events` | 다회 부분 취소·중복 전달 | PLANNED |
| TP-WEBHOOK-003 | `DEPOSIT_CALLBACK` | 가상계좌 입금·입금 취소 결과 전달 | `toss-payments-webhook-guide`, `toss-payments-virtual-account-webhook` | 입금·입금 취소·역순 | PLANNED |
| TP-WEBHOOK-004 | 전달 정책 | 10초 안에 200이면 성공, 실패 시 문서의 간격으로 최대 7회 재전송 | `toss-payments-webhook-guide` | fake clock 기반 1·4·16·64·256·1024·4096분 | PLANNED |

## 샌드박스 전용 제어 계약

| ID | 인터페이스 | 목적 | 상태 |
| --- | --- | --- | --- |
| SBX-001 | `POST /sandbox/v1/payment-sessions` | 합성 인증 결과와 `paymentKey` 생성 | PLANNED |
| SBX-002 | `POST /sandbox/v1/virtual-accounts/{paymentKey}/deposits` | 입금·입금 취소와 웹훅 발생 | PLANNED |
| SBX-003 | `PUT /sandbox/v1/fault-profile` | 승인 거절·응답 유실·timeout·웹훅 실패를 seed로 고정 | PLANNED |

샌드박스 API는 test profile과 로컬 데모에서만 활성화하고 실제 개인정보·결제수단 정보를 받지 않습니다.

## 완료 규칙

- 한 행은 구현 코드, 자동화 테스트 ID와 evidence manifest가 모두 연결되어야 `VERIFIED`가 됩니다.
- 공식 문서가 바뀌면 source `checkedAt`, schema snapshot, 영향 행과 회귀 테스트를 함께 갱신합니다.
- 부분 호환 결과는 전체 토스페이먼츠 API 호환으로 표현하지 않습니다.
