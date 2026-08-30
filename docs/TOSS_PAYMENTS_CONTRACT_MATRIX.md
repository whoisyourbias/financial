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
| TP-VA-001 | `POST /v1/virtual-accounts` | 필수 `amount`, `orderId`, `orderName`, `customerName`, `bank`로 발급하고 입금 전 상태를 Payment로 표현 | `toss-payments-core-api`, `toss-payments-virtual-account` | 발급·필수값 누락·중복 주문·잘못된 은행·만료 | PLANNED |
| TP-VA-002 | `POST /v1/payments/{paymentKey}/cancel`의 가상계좌 규칙 | 입금 전에는 `refundReceiveAccount` 없이 전액 취소만 허용하고, 입금 후 취소에는 유효한 `bank`, `accountNumber`, `holderName`을 포함한 `refundReceiveAccount` 필수 | `toss-payments-core-api`, `toss-payments-cancel-guide` | 입금 전 전액·입금 전 부분 거절·입금 후 환불계좌 정상/누락/유효성 실패 | PLANNED |

## 공통 wire 계약

| ID | 대상 | 규약 | sourceRefs | 계획 검증 | 상태 |
| --- | --- | --- | --- | --- | --- |
| TP-WIRE-001 | 공개 인증 wire | 시크릿 키를 username, 빈 문자열을 password로 둔 `Basic base64(secret:)` Authorization 헤더 | `toss-payments-api-keys`, `toss-payments-authorization` | 정상·누락·scheme/Base64 오류·콜론 누락·잘못된 시크릿 | PLANNED |
| TP-WIRE-002 | 공개 멱등성 | 모든 POST에서 `Idempotency-Key + API key + API path + HTTP method` 조합을 15일간 적용; 최대 300자, 확정 재요청은 최초 응답, 처리 중 재요청은 `409 IDEMPOTENT_REQUEST_PROCESSING` | `toss-payments-authorization`, `toss-payments-idempotency` | 같은 tuple 재전송·API key/path/method 분리·길이 초과 400·처리 중 409; body-hash 충돌은 sandbox 정책으로 분리 | PLANNED |
| TP-WIRE-003 | 응답 | JSON 리소스 또는 `code`·`message` Error와 HTTP 상태 | `toss-payments-request-response`, `toss-payments-error-codes` | endpoint별 대표 4xx·5xx fixture | PLANNED |
| TP-WIRE-004 | 버전 | additive 변경과 파괴적 변경을 구분하고 구버전 소비자를 보호 | `toss-payments-versioning` | old/new contract 조합 | PLANNED |
| TP-WIRE-005 | Payment | 공통 필드와 method별 `card`, `easyPay`, `virtualAccount`, `cancels`, `failure` | `toss-payments-core-api`, `toss-payments-data-types`, `toss-payments-enum-codes` | schema snapshot과 JSON fixture | PLANNED |
| TP-WIRE-006 | 비범위 표기 | 세금·해외통화·에스크로 등은 지원 목록에서 제외하고 해당 요청의 Toss 호환 동작을 주장하지 않음 | `toss-payments-core-api` | 지원·비지원 fixture 목록과 샌드박스 자체 오류의 별도 표기 | PLANNED |

## 상태와 웹훅 계약

| ID | 대상 | 규약 | sourceRefs | 계획 검증 | 상태 |
| --- | --- | --- | --- | --- | --- |
| TP-STATE-001 | Payment 상태 | `READY`, `IN_PROGRESS`, `WAITING_FOR_DEPOSIT`, `DONE`, `CANCELED`, `PARTIAL_CANCELED`, `ABORTED`, `EXPIRED` | `toss-payments-core-api`, `toss-payments-enum-codes` | 결제수단별 허용·불허 전이 | PLANNED |
| TP-WEBHOOK-001 | `PAYMENT_STATUS_CHANGED` | body의 `eventType`, `createdAt`, Payment `data`와 `tosspayments-webhook-transmission-time`, `tosspayments-webhook-transmission-retried-count`, `tosspayments-webhook-transmission-id` 헤더를 MID별 endpoint로 전달 | `toss-payments-webhook-guide`, `toss-payments-webhook-events` | 정상·반복 전달·늦은 전달·헤더 누락; retry 사이 transmission ID 안정성은 주장하지 않음 | PLANNED |
| TP-WEBHOOK-002 | `DEPOSIT_CALLBACK` | body의 `createdAt`, `secret`, `status`, `transactionKey`, `orderId`와 공통 transmission 헤더를 전달하고 승인 Payment의 `secret`과 일치 검증 | `toss-payments-webhook-guide`, `toss-payments-virtual-account-webhook` | 입금·입금 취소·secret 불일치·역순 | PLANNED |
| TP-WEBHOOK-003 | 가상계좌 이중 전달 | `PAYMENT_STATUS_CHANGED`와 `DEPOSIT_CALLBACK`을 모두 등록하면 같은 가상계좌 상태 변경에 웹훅이 두 번 전송됨 | `toss-payments-webhook-events` | 두 event type이 각각 전달되는 fixture와 event별 body 검증 | PLANNED |
| TP-WEBHOOK-004 | 전달 정책 | 10초 안에 200이면 성공, 실패 시 문서의 간격으로 최대 7회 재전송 | `toss-payments-webhook-guide` | fake clock 기반 1·4·16·64·256·1024·4096분 | PLANNED |

`CANCEL_STATUS_CHANGED`는 일반 국내 결제에 발송되지 않고 해외 간편결제 취소 또는 취소 실패에 사용되는 이벤트이므로 선정 범위에서 제외합니다. 국내 취소 상태는 취소 API 응답·Payment 조회와 `PAYMENT_STATUS_CHANGED`의 Payment 상태로 검증합니다.

## 프로젝트 내부 안전 계약

아래 행은 공식 토스페이먼츠 계약이 아니라 샌드박스와 샘플 상점이 공개 이벤트를 안전하게 처리하기 위한 내부 결정입니다.

| ID | 대상 | 내부 규약 | 계획 검증 | 상태 |
| --- | --- | --- | --- | --- |
| INT-AUTH-001 | credential 수명주기·격리 | 시크릿 원문을 저장하지 않고 hash를 MID·test/live-simulated 환경에 묶으며, 회전 grace·즉시 폐기·캐시 무효화와 사람 역할을 공개 Basic wire 계약과 별도로 관리 | MID/환경 교차 사용·회전 전후·폐기 뒤 캐시·역할 오용 | PLANNED |
| INT-WEBHOOK-001 | Payment 반복 전달 | `eventType`, `Payment.paymentKey`, status와 선정 Payment 필드의 canonical hash가 같은 snapshot은 상점 business effect를 다시 만들지 않음 | 서로 다른 transmission ID·time으로 같은 snapshot 반복, 변경 snapshot 구분 | PLANNED |
| INT-WEBHOOK-002 | 가상계좌 교차 이벤트 | guarded Payment 상태 전이와 입금 `transactionKey`로 두 event type의 도메인 입금·분개·알림 효과를 한 번만 적용하고 외부 전달 이력은 각각 보존 | 두 event type의 양방향 순서·반복 전달 | PLANNED |

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
