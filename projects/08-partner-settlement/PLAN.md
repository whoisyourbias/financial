# 프로젝트 08 — 외부 파트너·웹훅·정산

| 항목           | 값                                                             |
| -------------- | -------------------------------------------------------------- |
| 기간           | 2026-12-07 ~ 2026-12-20                                        |
| 상태           | PLANNED                                                        |
| 선행 프로젝트  | 03, 04, 07                                                     |
| 목표 태그      | `p08-partner-settlement`                                       |
| 주력 채용 신호 | 외부 API, webhook 보안, retry, rate limit, clearing·settlement |

## 문제 정의

외부 결제 파트너는 요청 timeout 뒤 실제로 처리했을 수 있고, webhook을 중복·지연·순서가 바뀐 상태로 보낼 수 있습니다. 파트너의 성공 응답과 내부 원장, clearing 파일이 서로 다를 수도 있습니다. 이 프로젝트는 외부 시스템을 신뢰할 수 없는 경계로 보고 요청·조회·webhook·정산을 함께 사용해 최종 상태를 확정합니다.

파트너는 합성 stub이며 실제 계약·PG 운영 경험으로 표현하지 않습니다.

## 필수 범위

- versioned partner client와 contract test
- connection·read timeout, retry, exponential backoff·jitter
- retry 가능한 오류와 불가능한 오류 구분
- 요청 idempotency와 partner reference
- HMAC 기반 webhook 서명·timestamp·replay window
- 중복·순서 역전 webhook 처리
- inbound·outbound rate limit과 제한 응답
- `UNKNOWN` 또는 `PENDING_CONFIRMATION` 상태와 status query
- clearing file ingestion과 payment·ledger 대사
- settlement difference 분류와 재처리 runbook

## 비범위

- 실제 PG credential·endpoint
- 카드망 전문, PCI DSS
- 다중 파트너 라우팅 최적화
- 환율·세금·수수료 정산
- 자동 차이 금전 보정
- 파트너 SLA·상용 성공률 주장

## 외부 경계 불변식

1. timeout만으로 성공·실패를 임의 확정하지 않습니다.
2. 같은 partner reference·idempotency key는 한 결제 operation을 가리킵니다.
3. 유효하지 않거나 오래된 서명의 webhook은 상태를 변경하지 않습니다.
4. 중복 webhook은 한 상태 전이만 만듭니다.
5. 오래된 webhook은 최신 aggregate version을 되돌리지 않습니다.
6. clearing record와 내부 payment·journal의 차이는 삭제되지 않고 분류됩니다.
7. retry는 상한과 전체 시간 budget을 가집니다.

## 설계 후보

### timeout 뒤 처리

- 즉시 실패 확정
- 동일 키로 재호출
- status query로 확인 후 재호출

파트너가 처리했을 수 있으므로 `PENDING_CONFIRMATION + status query`를 기준 후보로 삼고 stub 시나리오로 검증합니다.

### retry 위치

- HTTP client 내부 자동 retry
- application workflow의 명시적 retry
- queue 기반 비동기 retry

오류 의미와 전체 time budget을 application이 알 수 있도록 경계를 문서화합니다. 중첩 retry로 시도 횟수가 곱해지지 않게 합니다.

## 계획 인터페이스

```text
POST /api/v1/partner-stub/payments
GET  /api/v1/partner-stub/payments/{reference}
POST /api/v1/webhooks/payment-partner
POST /api/v1/settlement-files
GET  /api/v1/settlements/{id}/differences
POST /api/v1/settlements/{id}/reprocess
```

stub API는 운영 API와 package·credential을 분리하고 production profile에서 활성화되지 않게 합니다.

## 데이터 흐름

```text
payment operation
  → signed/idempotent partner request
  → response 또는 timeout
  → pending confirmation
  → signed webhook / status query
  → versioned state transition
  → clearing file
  → payment + ledger reconciliation
  → difference queue / controlled reprocess
```

## 산출물

- deterministic partner stub과 fault scenario
- partner client·contract test
- webhook signature·replay 방어
- timeout·retry·rate limit 정책
- pending confirmation과 status query
- clearing·settlement batch와 차이 보고서
- partner outage·reprocess runbook

## 10일 작업 계획

| 일차 | 작업                              | 완료 기준                           |
| ---- | --------------------------------- | ----------------------------------- |
| 1    | 외부 실패·불명확 상태·불변식      | timeout·webhook·settlement 시나리오 |
| 2    | retry·signature·confirmation ADR  | time budget과 공격 경계 확정        |
| 3    | partner stub·contract             | deterministic fault mode            |
| 4    | client timeout·retry·rate limit   | 중첩 retry·무한 재시도 방지         |
| 5    | webhook signature·replay          | 위조·오래된·중복 거절               |
| 6    | pending confirmation·status query | timeout 뒤 최종 상태 확정           |
| 7    | clearing·settlement 대사          | payment·journal 차이 분류           |
| 8    | outage·forgery·reorder 실험       | 원본 timeline·정합성 생성           |
| 9    | runbook·ADR·evidence              | 파트너 stub limitation 명시         |
| 10   | Portfolio Review·Red Team         | PASS와 🔴 0건 또는 HOLD             |

## 테스트와 측정

- connect timeout, read timeout, 4xx, 5xx
- retry-after와 rate limit 초과
- 처리 성공 뒤 응답 유실
- 동일 요청 중복·다른 payload 재사용
- 잘못된 signature·timestamp·body tampering
- webhook duplicate·out-of-order·late arrival
- status query와 webhook 경쟁
- clearing 누락·중복·금액 mismatch
- reprocess 중복 실행

측정 항목:

- partner attempt 수와 고유 operation 수
- retry 원인·상한·전체 elapsed time
- webhook 수신·유효·거절·중복 억제 수
- pending confirmation 체류 시간과 최종 결과
- clearing matched·difference 수와 금액

## 필요한 증거

- stub fault scenario와 seed
- contract test와 request·response schema
- signature canonical string과 replay window
- timeout 뒤 partner·internal state timeline
- retry attempt와 total time budget 원본
- webhook·payment operation·journal 대조
- settlement individual·aggregate report
- 실제 PG 미연동 limitation

## 예상 면접 질문

1. timeout을 실패로 확정하면 이중 결제가 생길 수 있는 이유는 무엇인가요?
2. webhook 서명에 timestamp와 replay window가 필요한 이유는 무엇인가요?
3. HTTP client와 application 양쪽 retry가 겹치면 어떤 문제가 생기나요?
4. 늦게 도착한 승인 webhook이 취소 상태를 되돌리지 않게 어떻게 막나요?
5. 파트너 응답과 clearing 결과가 다르면 무엇을 source of truth로 봅니까?

## 주요 위험과 다음 회차 연결

- retry를 신뢰성 향상의 단일 원인으로 과장하지 않습니다.
- webhook 수신 시간을 결제 완료시간으로 바꿔 쓰지 않습니다.
- HMAC 구현을 PCI 준수로 표현하지 않습니다.
- 프로젝트 09는 이 파트너 실패 경계 때문에 결제 모듈을 분리할 가치가 있는지 실제 비교합니다.
