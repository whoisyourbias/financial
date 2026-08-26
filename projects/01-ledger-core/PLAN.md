# 프로젝트 01 — 복식부기 원장 코어

| 항목           | 값                                            |
| -------------- | --------------------------------------------- |
| 기간           | 2026-08-31 ~ 2026-09-13                       |
| 상태           | PLANNED                                       |
| 선행 프로젝트  | 없음                                          |
| 목표 태그      | `p01-ledger-core`                             |
| 주력 채용 신호 | 금융 불변식, DB 모델링, 트랜잭션, 테스트 설계 |

## 문제 정의

잔액 숫자만 직접 갱신하면 왜 돈이 변했는지 추적하기 어렵고, 일부 갱신 실패나 수정으로 총액이 어긋날 수 있습니다. 첫 프로젝트는 금전 변화의 원인을 불변 journal과 debit/credit posting으로 기록하고, 모든 거래에서 차변과 대변 합계가 일치하도록 데이터베이스와 애플리케이션 양쪽에서 방어합니다.

이 프로젝트가 증명하려는 범위는 `개인 학습용 원장 코어의 정합성`입니다. 은행 전체 계정계, 회계 결산, 다통화 환산 또는 규제 준수를 완성했다고 주장하지 않습니다.

## 필수 범위

- account, journal entry, posting의 용어와 책임 정의
- 자산·부채·수익·비용·자본 계정 유형의 최소 모델
- `Money(amountMinor, currency)` 값 객체
- 하나의 journal entry에 2개 이상의 posting 기록
- 차변·대변 합계가 다른 요청 거절
- 확정된 journal·posting의 update/delete 금지
- 외부 참조 ID 중복 거절
- 계정별 posting 조회와 특정 시점 잔액 조회
- PostgreSQL migration과 Testcontainers 통합 테스트

## 비범위

- 외화 환산과 환율 손익
- 이자·세금·회계 마감
- 계정 간 이체 API
- event sourcing 프레임워크
- 실제 회계 기준 인증
- 성능 최적화 또는 대규모 처리 주장

## 금융 불변식

1. 하나의 journal entry에서 debit 합계와 credit 합계는 같은 통화 기준으로 동일합니다.
2. posting 금액은 0보다 커야 합니다.
3. 서로 다른 통화의 posting을 하나의 entry에서 상계하지 않습니다.
4. 확정된 journal과 posting은 수정·삭제하지 않습니다. 정정은 새 반대 posting으로 처리합니다.
5. 동일한 외부 참조는 같은 결과를 가리키며 다른 내용으로 재사용할 수 없습니다.
6. 계정의 시점 잔액은 해당 시점까지의 posting으로 재계산할 수 있어야 합니다.

## 설계 후보와 실제 비교 항목

### 잔액 저장 방식

- 후보 A: account row의 balance 직접 갱신
- 후보 B: posting 합계로 매번 계산
- 후보 C: posting을 source of truth로 두고 별도 balance projection 유지

프로젝트 01에서는 B를 기준 구현으로 사용하고, C는 프로젝트 02에서 경합·조회 비용과 함께 검토합니다. A는 감사 추적과 정정 비용을 문서로 분석하되 실제로 비교하지 않았다면 `미실험`이라고 표시합니다.

### 불변성 강제 위치

- 애플리케이션 command validation
- 데이터베이스 constraint·trigger·권한
- append-only repository 정책

모든 규칙을 DB trigger에 숨기지 않습니다. 애플리케이션 오류 설명과 DB 최종 방어의 역할을 구분하고, 실제 적용한 규칙만 ADR에 적습니다.

## 계획 인터페이스

```text
POST /api/v1/journal-entries
GET  /api/v1/journal-entries/{journalEntryId}
GET  /api/v1/accounts/{accountId}/postings?from=&to=&cursor=
GET  /api/v1/accounts/{accountId}/balance?at=
```

변경 요청은 `Idempotency-Key`를 받습니다. 요청은 설명, 외부 참조, 발생 시각, posting 목록을 포함합니다. API 응답의 합계와 통화는 저장된 posting에서 계산합니다.

## 데이터 흐름

```text
HTTP command
  → 요청·통화·posting 검증
  → journal application service
  → 하나의 DB transaction에서 journal + postings 저장
  → commit
  → journal과 account query로 재조회
```

프로젝트 03 전에는 broker나 outbox를 추가하지 않습니다.

## 산출물

- 도메인 용어집과 계정 유형 설명
- ERD와 현재 구현 아키텍처
- Flyway migration
- 원장 command/query API
- 단위·통합·생성형 테스트
- 잔액 저장 후보 ADR
- evidence manifest와 결과 문서

## 10일 작업 계획

| 일차 | 작업                              | 완료 기준                      |
| ---- | --------------------------------- | ------------------------------ |
| 1    | 용어·불변식·비범위 확정           | 문장별 테스트 가능성 확인      |
| 2    | ERD, Money, 잔액 후보 ADR         | 구현 전 가정과 미실험 구분     |
| 3    | 프로젝트 골격, migration, account | 컨테이너 DB에서 migration 성공 |
| 4    | journal·posting 도메인            | 불균형·통화 혼합 단위 테스트   |
| 5    | command API와 transaction         | 정상 entry 원자적 저장         |
| 6    | query·시점 잔액                   | posting으로 잔액 재계산        |
| 7    | append-only·중복 참조 방어        | update/delete·중복 실패 테스트 |
| 8    | 생성형·실패·migration 검증        | 원장 불변식 위반 0건           |
| 9    | evidence, ERD, 결과·한계          | 표와 원본 명령 연결            |
| 10   | Portfolio Review·Red Team         | PASS와 🔴 0건 또는 HOLD        |

## 테스트와 측정

- Money의 통화 불일치·overflow 경계
- debit/credit 불균형, 빈 posting, 음수·0 금액
- journal 저장 중 posting 실패 시 전체 rollback
- 동일 외부 참조의 같은 요청과 다른 요청
- 확정 record의 update/delete 시도
- 무작위 균형 posting 생성 후 합계와 시점 잔액 검증
- migration을 빈 DB와 이전 migration 상태에서 실행

성능 숫자를 대표 성과로 사용하지 않습니다. 이 회차의 핵심 결과는 정합성과 재현 가능한 모델입니다.

## 필요한 증거

- 불변식별 테스트 이름과 실행 결과
- 불균형 요청의 API·DB 거절 근거
- rollback 전후 row count와 journal 상태
- 무작위 테스트 seed와 case 수
- migration 버전과 실행 명령
- ERD의 table·column과 실제 schema 대조
- 사용하지 않은 회계 기능 목록

## 예상 면접 질문

1. account balance를 row에 바로 저장하지 않은 이유와 그 비용은 무엇인가요?
2. 애플리케이션 검증을 우회한 insert도 차변·대변 불변식을 지킬 수 있나요?
3. 확정 거래를 삭제하지 않고 정정하려면 어떤 posting을 추가합니까?
4. 서로 다른 통화를 하나의 journal entry에서 어떻게 다룹니까?
5. 이 구현을 event sourcing이라고 부르지 않는 이유는 무엇인가요?

## 주요 위험과 다음 회차 연결

- posting 합계 조회가 커지면 느려질 수 있습니다. 프로젝트 02에서 projection 후보를 실제 경합과 함께 검토합니다.
- 복식부기 용어를 얕게 쓰지 않도록 범위를 단순 거래 기록으로 제한합니다.
- 애플리케이션 검증만으로 끝내지 않고 가능한 DB 최종 방어를 확인합니다.
- 프로젝트 02는 이 원장 port를 통해 이체를 기록하며 ledger table에 직접 접근하지 않습니다.
