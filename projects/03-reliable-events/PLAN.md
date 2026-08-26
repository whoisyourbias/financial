# 프로젝트 03 — 신뢰 가능한 이벤트 처리

| 항목           | 값                                           |
| -------------- | -------------------------------------------- |
| 기간           | 2026-09-28 ~ 2026-10-11                      |
| 상태           | PLANNED                                      |
| 선행 프로젝트  | 01 원장, 02 이체                             |
| 목표 태그      | `p03-reliable-events`                        |
| 주력 채용 신호 | 메시징, 멱등 소비, 장애 복구, event contract |

## 문제 정의

DB transaction이 성공한 뒤 broker publish가 실패하면 이체는 완료됐지만 후속 시스템은 이를 알지 못합니다. 반대로 publish 재시도는 같은 이벤트를 여러 번 전달할 수 있습니다. 이 프로젝트는 DB 변경과 발행 의도를 함께 저장하고, 중복·지연·순서 역전을 정상적인 전달 특성으로 처리합니다.

목표는 `exactly-once`가 아니라 **at-least-once 전달에서 한 비즈니스 결과를 유지하는 소비자**입니다.

## 필수 범위

- 성공 transfer와 journal event 계약
- 비즈니스 transaction과 같은 DB transaction의 outbox record
- outbox publisher의 재시도·lease·상태 관리
- Kafka 호환 broker로 publish
- consumer inbox 또는 processed-event 기록
- 중복 event 억제
- retry 상한, DLQ, operator replay
- schema version과 하위 호환 규칙
- correlation ID 기반 HTTP→DB→broker→consumer 추적

## 비범위

- broker 자체의 고가용성 운영
- 글로벌 event ordering
- exactly-once 처리 주장
- event sourcing 전환
- 모든 모듈의 비동기화
- CDC 플랫폼 운영 경험 주장

## 전달 불변식

1. commit된 발행 의도는 outbox에 남아 재시도할 수 있습니다.
2. rollback된 transfer의 이벤트는 publish되지 않습니다.
3. 같은 `eventId`가 여러 번 도착해도 consumer side effect는 한 비즈니스 결과만 만듭니다.
4. poison event는 무한 재시도하지 않고 격리됩니다.
5. replay는 원본 `eventId`와 replay 실행 기록을 보존합니다.
6. consumer는 event 순서에 의존하면 aggregate version을 검증합니다.

## 설계 후보

### DB commit 후 직접 publish

구현은 단순하지만 commit과 publish 사이 crash window가 있습니다. failure baseline으로 재현합니다.

### Transactional outbox

비즈니스 변경과 발행 의도를 원자적으로 저장할 수 있지만 polling, 중복 전달, cleanup, lag가 새 비용입니다. 이 프로젝트의 기준 구현 후보입니다.

### CDC 기반 outbox

polling을 줄일 수 있지만 connector 운영과 schema 의존 비용이 생깁니다. 2주 범위에는 구현하지 않으며, 실제 도입한 것처럼 비교하지 않습니다.

## 계획 이벤트

```text
transfer.completed.v1
journal.entry.posted.v1
```

공통 envelope는 `eventId`, `eventType`, `schemaVersion`, `occurredAt`, `correlationId`, `aggregateId`, `payload`를 포함합니다. 개인정보를 event payload에 복제하지 않습니다.

## 데이터 흐름

```text
transfer transaction
  → transfer + journal + outbox commit
  → publisher가 outbox claim
  → broker publish
  → consumer 수신
  → inbox에서 eventId 확인
  → read model 또는 알림 side effect
  → inbox + side effect commit
```

publish 성공 표시 전에 프로세스가 죽는 경우와 consumer commit 뒤 offset commit 전에 죽는 경우를 각각 주입합니다.

## 산출물

- versioned event schema와 compatibility 규칙
- outbox publisher와 retry·lease
- inbox 기반 idempotent consumer
- DLQ 조회·replay 운영 API 또는 명령
- direct publish failure baseline
- 전달 lag·중복·복구 결과
- runbook 초안과 ADR

## 10일 작업 계획

| 일차 | 작업                             | 완료 기준                      |
| ---- | -------------------------------- | ------------------------------ |
| 1    | 전달 실패 메커니즘·불변식        | 두 crash window 시나리오 확정  |
| 2    | 직접 publish baseline·outbox ADR | 구현·미구현 후보 구분          |
| 3    | event envelope·schema test       | v1 직렬화·호환 fixture         |
| 4    | outbox 저장과 publisher          | transfer commit에 의도 포함    |
| 5    | retry·lease·cleanup              | 다중 publisher claim 충돌 방어 |
| 6    | inbox consumer                   | 중복 side effect 억제          |
| 7    | DLQ·replay·관측 필드             | poison event 격리·재처리       |
| 8    | crash·duplicate·ordering 실험    | delivery·side effect 원본 생성 |
| 9    | runbook·ADR·evidence             | 측정 지점·한계 명시            |
| 10   | Portfolio Review·Red Team        | PASS와 🔴 0건 또는 HOLD        |

## 테스트와 측정

- transfer rollback과 outbox 부재
- transfer commit 직후 publisher 종료
- publish 성공 후 status update 전 종료
- 동일 event 중복·동시 소비
- consumer side effect commit 뒤 종료
- poison payload와 retry 상한
- 이전·미래 schema version 처리
- aggregate version 순서 역전
- DLQ replay 중복 실행

측정 항목:

- commit된 outbox 수, publish attempt·success 수
- 고유 event 수, delivery 수, duplicate 수
- 고유 side effect 수와 중복 억제 수
- commit부터 첫 성공 consume까지 delivery lag p50/p95/p99
- 장애 주입부터 backlog 0까지 recovery duration

## 필요한 증거

- direct publish crash에서 유실을 재현한 baseline
- outbox와 transfer commit의 transaction 근거
- broker record와 inbox·side effect query
- retry·DLQ·replay 원본 로그
- lag 측정의 clock·시작·종료 정의
- event schema compatibility test
- `at-least-once` 용어와 남은 한계

## 예상 면접 질문

1. outbox가 exactly-once를 보장하지 않는 이유는 무엇인가요?
2. publish는 성공했지만 outbox 상태 갱신이 실패하면 어떻게 됩니까?
3. inbox 기록과 side effect가 다른 transaction이면 어떤 문제가 생기나요?
4. DLQ replay가 다시 side effect를 만들지 않게 어떻게 방어합니까?
5. 모든 이벤트의 순서를 보장하지 않고도 도메인 정합성을 지키는 방법은 무엇인가요?

## 주요 위험과 다음 회차 연결

- broker를 도입했다는 이유로 SPOF가 제거됐다고 주장하지 않습니다.
- backlog cleanup과 table growth는 새 운영 비용으로 기록합니다.
- correlation ID가 곧 distributed transaction을 의미하지 않습니다.
- 프로젝트 04는 event와 외부 거래 파일을 원장과 대사합니다.
