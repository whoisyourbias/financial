# 프로젝트 09 — 결제 서비스 분리 실험

| 항목           | 값                                                      |
| -------------- | ------------------------------------------------------- |
| 기간           | 2026-12-21 ~ 2027-01-03                                 |
| 상태           | PLANNED                                                 |
| 선행 프로젝트  | 03, 06~08                                               |
| 목표 태그      | `p09-service-extraction`                                |
| 주력 채용 신호 | 아키텍처 트레이드오프, 서비스 계약, 분산 실패, rollback |

## 문제 정의

결제 모듈은 외부 파트너 장애, webhook, 독립 배포 가능성이라는 고유한 경계를 가집니다. 그러나 프로세스를 분리하면 네트워크 실패, 분산 정합성, 운영·테스트·배포 비용이 새로 생깁니다. 이 프로젝트는 모듈 상태와 서비스 분리 상태를 같은 use case에서 비교해 분리가 실제로 이득인지 검증합니다.

결론은 미리 정하지 않습니다. 분리 비용이 크면 모듈 유지 또는 원복이 성공적인 결과입니다. 서비스 하나를 분리했다고 `MSA 운영 경험`으로 표현하지 않습니다.

## 필수 범위

- 비교 전 modular payment 기준선 고정
- payment process와 database ownership 분리 prototype
- core↔payment versioned synchronous API
- payment event와 core consumer contract
- 원장 반영 실패·지연 시 보상 또는 pending 상태
- network timeout·partition·duplicate·reorder
- API·event backward compatibility contract test
- local compose와 CI 실행 복잡도 기록
- 분리 전후 변경 영향·지연·복구·운영비 비교
- 원복 절차와 실제 rollback rehearsal

## 비범위

- 전체 시스템 MSA 전환
- Kubernetes·service mesh
- 독립 팀 구조·조직 확장성 주장
- distributed transaction을 숨기는 framework 도입
- 분리 결과가 무조건 더 낫다는 결론
- 상용 multi-service 운영 경험 주장

## 실험 질문

1. 파트너 adapter 변경이 core banking 배포와 분리되는가?
2. 결제 장애가 원장·이체 API의 failure domain에서 격리되는가?
3. end-to-end 지연과 불명확 상태가 얼마나 늘어나는가?
4. 로컬 실행, contract test, 관측, 배포 단계가 얼마나 복잡해지는가?
5. payment DB와 ledger DB의 불일치를 어떤 상태·대사·보상으로 복구하는가?
6. 작은 개인 프로젝트에서 이 분리가 유지할 가치가 있는가?

## 비교 조건

분리 전후 다음을 동일하게 유지합니다.

- payment create→authorize→capture use case
- partner stub fault schedule
- dataset·request mix·duration
- 기능·정합성 assertion
- 관측 지표의 정의
- 실행 장비 또는 명시적인 topology 차이

코드 줄 수처럼 해석이 약한 숫자 하나로 결론 내리지 않습니다. 변경 파일 범위, build·startup 단계, failure point, latency, runbook 단계, 데이터 복구 절차를 함께 봅니다.

## 서비스 계약

```text
Core Banking
  → POST Payment Service /payments/{id}/captures
  ← payment operation result or pending

Payment Service
  → payment.captured.v1
  → payment.refunded.v1

Core Banking
  → ledger posting consumer
  → ledger.applied / ledger.rejected result
```

원장 반영 전 payment를 최종 완료로 볼지, `CAPTURE_PENDING_LEDGER`로 둘지 ADR에서 결정합니다. 선택과 상관없이 불명확 상태를 숨기지 않습니다.

## 데이터 소유권

- Payment Service: payment, operation, partner reference, webhook
- Core Banking: journal, posting, account balance
- 공유 DB table 접근 금지
- 식별자와 versioned contract만 공유

## 산출물

- 분리 전 기준선과 현재 diagram
- payment service·DB prototype
- synchronous API·event contract
- idempotent core consumer와 보상·pending 흐름
- network fault·schema compatibility test
- 분리 전후 비교표
- rollback runbook과 실제 rehearsal
- 유지·분리·원복 중 최종 결론 ADR

## 10일 작업 계획

| 일차 | 작업                              | 완료 기준                       |
| ---- | --------------------------------- | ------------------------------- |
| 1    | 비교 질문·기준선·동일 조건        | 분리 전 evidence 고정           |
| 2    | ownership·contract·보상 ADR       | 결론은 `[가정]` 유지            |
| 3    | payment process·DB 분리           | 공유 table 접근 없음            |
| 4    | synchronous API·idempotency       | timeout·retry 계약              |
| 5    | versioned payment event           | core consumer 중복 안전         |
| 6    | ledger pending·보상·대사          | 두 DB 불일치 상태 노출          |
| 7    | contract·network·schema tests     | partition·duplicate·구버전 검증 |
| 8    | 분리 전후 실행·rollback rehearsal | 비교 원본과 원복 성공           |
| 9    | 결과·비용·ADR·evidence            | 유지/분리 결론과 한계           |
| 10   | Portfolio Review·Red Team         | PASS와 🔴 0건 또는 HOLD         |

## 테스트와 측정

- payment API timeout 전·후 서버 처리
- payment event duplicate·reorder
- ledger consumer 실패·재시작
- payment 완료와 ledger 실패
- old/new schema producer·consumer 조합
- payment service stop이 core 이체에 미치는 영향
- local compose·CI 전체 시작과 test 단계
- 분리 상태에서 module 상태로 rollback

비교 항목:

- end-to-end latency와 pending duration
- 실패 지점 수와 runbook 단계
- deployable·database·contract 수
- 단일 변경의 영향 파일·테스트 범위
- 불일치 탐지·복구 시간
- local 자원·AWS 예상 비용

## 필요한 증거

- 분리 전후 동일 use case·workload
- 두 architecture diagram과 실제 process·DB 목록
- API·event contract test 결과
- network partition·ledger failure timeline
- payment·ledger 불일치와 대사 결과
- rollback 명령·데이터·test 결과
- 결론을 바꾸게 한 근거와 감수한 비용

## 예상 면접 질문

1. 모듈로 유지하는 것보다 서비스로 분리한 실제 이점은 무엇이었나요?
2. payment 성공과 ledger 실패 사이 상태를 사용자에게 어떻게 표현합니까?
3. DB를 공유하면 분리는 쉬워지는데 왜 금지했나요?
4. contract version을 변경할 때 old consumer를 어떻게 보호합니까?
5. 분리 비용이 더 컸다면 왜 원복하거나 유지했나요?

## 주요 위험과 다음 회차 연결

- 이미 정한 분리 결론을 정당화하기 위해 숫자를 선택하지 않습니다.
- latency만 보고 결합도·복구·운영비를 무시하지 않습니다.
- 하나의 분리 실험을 MSA·분산 시스템 운영 경력으로 과장하지 않습니다.
- 프로젝트 10의 FDS는 결제 이벤트를 비동기로 소비하지만 결제 성공을 막지 않습니다.
