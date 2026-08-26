# 프로젝트 02 — 계좌이체와 동시성

| 항목           | 값                                  |
| -------------- | ----------------------------------- |
| 기간           | 2026-09-14 ~ 2026-09-27             |
| 상태           | PLANNED                             |
| 선행 프로젝트  | 01 원장                             |
| 목표 태그      | `p02-transfer-concurrency`          |
| 주력 채용 신호 | 트랜잭션, 동시성, 멱등성, 병목 분석 |

## 문제 정의

같은 계좌에서 동시에 여러 출금이 일어나면 각 요청이 같은 잔액을 읽고 모두 성공해 잔액 한도를 넘길 수 있습니다. 네트워크 재시도로 동일 이체가 중복 실행될 수도 있습니다. 이 프로젝트는 자금 보존, 사용 가능 잔액, 중복 요청의 동일 결과를 명시하고 서로 다른 동시성 제어 방식의 비용을 같은 workload에서 비교합니다.

목표는 특정 잠금 방식의 우월성을 선언하는 것이 아니라 충돌 패턴에 따라 선택 기준이 달라지는 이유를 증거로 설명하는 것입니다.

## 필수 범위

- 출금 계좌, 입금 계좌, 금액, 외부 참조를 가진 transfer command
- 원장 posting을 통한 이체 기록
- 사용 가능 잔액이 부족한 이체 거절
- `Idempotency-Key`의 같은 요청 재시도와 다른 본문 충돌
- transfer 상태와 결과 조회
- hot-account와 분산-account workload
- 낙관적 잠금, 비관적 잠금, 조건부 원자 갱신 중 최소 두 방식의 실제 비교
- timeout·deadlock·재시도 정책과 상한

## 비범위

- 은행 간 네트워크와 실제 계좌 검증
- 예약 이체, 수수료, 환율
- 분산 transaction
- 캐시를 source of truth로 사용
- 개인 장비 결과를 대규모 트래픽으로 표현

## 금융 불변식

1. 성공 이체 전후 모든 관련 계정의 총액은 같습니다.
2. 정책이 허용하지 않는 음수 사용 가능 잔액이 생기지 않습니다.
3. 하나의 transfer는 원장에 한 번의 비즈니스 결과만 만듭니다.
4. 동일 멱등 키와 같은 요청은 동일 transfer ID와 결과를 반환합니다.
5. 동일 멱등 키와 다른 요청은 어떤 금전 변경도 만들지 않습니다.
6. timeout을 받은 클라이언트가 재시도해도 결과를 조회할 수 있습니다.

## 비교 실험

### 후보 A — 비관적 잠금

- 이체 계좌를 정렬된 순서로 잠가 deadlock 위험을 줄입니다.
- 높은 경합에서 충돌 처리 흐름은 단순할 수 있으나 대기와 throughput 저하가 생길 수 있습니다.

### 후보 B — 낙관적 잠금

- version 충돌 시 제한된 횟수와 backoff로 재시도합니다.
- 낮은 경합에는 유리할 수 있으나 hot account에서 재시도 폭증 가능성이 있습니다.

### 후보 C — 조건부 원자 갱신

- `available_balance >= amount` 조건을 포함한 update와 영향 row 수로 성공을 판단합니다.
- 빠를 수 있지만 projection과 원장의 source of truth 관계가 복잡해질 수 있습니다.

최소 A와 B를 같은 DB·데이터·duration에서 구현·비교합니다. C를 구현하지 않으면 문서 비교 대상으로 승격하지 않습니다.

## 계획 인터페이스

```text
POST /api/v1/transfers            Idempotency-Key required
GET  /api/v1/transfers/{id}
GET  /api/v1/accounts/{id}/balance
```

transfer 생성 응답은 `COMPLETED`, `REJECTED`, `PROCESSING` 중 현재 상태를 반환합니다. HTTP timeout과 비즈니스 실패를 같은 상태로 취급하지 않습니다.

## 데이터 흐름

```text
transfer command
  → idempotency record 확인·예약
  → 계좌·통화·한도 검증
  → 선택한 동시성 전략으로 잔액 보호
  → ledger port로 debit/credit posting 기록
  → transfer 결과·idempotency 결과 확정
  → commit
```

원장 table을 transfer repository에서 직접 수정하지 않습니다.

## 산출물

- transfer aggregate·상태·오류 계약
- 멱등 키 저장과 payload fingerprint
- 동시성 전략 두 개의 실험 구현 또는 격리된 prototype
- hot/distributed workload 스크립트
- 정확성·지연·충돌·재시도 결과
- 잠금 전략 ADR와 남은 한계

## 10일 작업 계획

| 일차 | 작업                       | 완료 기준                          |
| ---- | -------------------------- | ---------------------------------- |
| 1    | 자금 보존·잔액·멱등 불변식 | 정상·중복·timeout 시나리오 확정    |
| 2    | 잠금 후보와 workload 설계  | 같은 조건 비교 프로토콜 고정       |
| 3    | transfer command·상태·오류 | 원장 port 외 직접 접근 없음        |
| 4    | 멱등 키·payload 충돌       | 같은 요청 replay, 다른 요청 reject |
| 5    | 비관적 잠금 구현           | 정렬 잠금과 timeout 테스트         |
| 6    | 낙관적 잠금 구현           | 충돌 재시도 상한 테스트            |
| 7    | 잔액 projection·조회 정합  | 원장 재계산과 비교 가능            |
| 8    | 동시성·timeout·장애 실험   | 불변식과 latency 원본 생성         |
| 9    | ADR·결과·한계·evidence     | workload 조건과 원본 연결          |
| 10   | Portfolio Review·Red Team  | PASS와 🔴 0건 또는 HOLD            |

## 테스트와 측정

- 잔액보다 큰 단일·동시 출금
- 같은 계좌를 출발·도착으로 사용하는 요청
- 양방향 이체의 lock ordering과 deadlock
- 같은 키·같은 payload 병렬 재요청
- 같은 키·다른 payload 충돌
- DB commit 직전·직후 클라이언트 timeout
- low-contention과 hot-account workload 분리

측정 항목:

- 성공·거절·timeout·오류 요청 수
- p50·p95·p99 API latency와 측정 지점
- lock wait, optimistic conflict, retry 수
- 시작·종료 총액과 음수 잔액 건수
- 동일 멱등 키당 고유 transfer·journal 수

## 필요한 증거

- 두 동시성 전략의 동일 환경 설정
- workload seed, 계좌 수, 초기 잔액, request mix, duration
- 원본 부하 결과와 DB query
- 총액·개별 계정·journal 수 대조
- deadlock·timeout 로그와 복구 결과
- 선택하지 않은 전략의 실제 실험 여부

## 예상 면접 질문

1. 왜 이 workload에서는 비관적 또는 낙관적 잠금이 더 적합했나요?
2. 계좌 두 개의 lock 순서를 고정하지 않으면 어떤 교착이 생기나요?
3. 클라이언트 timeout 뒤 이체 성공 여부를 어떻게 확인합니까?
4. idempotency key 저장 transaction이 이체 transaction과 분리되면 어떤 문제가 생기나요?
5. balance projection이 원장과 다르면 어느 값을 믿고 어떻게 복구합니까?

## 주요 위험과 다음 회차 연결

- 한 환경에서의 우세를 일반화하지 않습니다.
- retry를 무제한으로 두지 않고 최종 오류와 관측 지표를 정의합니다.
- `한 번만 실행` 대신 `중복 요청에도 한 비즈니스 결과`라고 표현합니다.
- 프로젝트 03은 성공 transfer의 이벤트 전달을 지연·중복 가능한 경계로 확장합니다.
