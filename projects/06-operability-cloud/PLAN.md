# 프로젝트 06 — 관측성·장애 대응·1차 클라우드 쇼케이스

| 항목           | 값                                                  |
| -------------- | --------------------------------------------------- |
| 기간           | 2026-11-09 ~ 2026-11-22                             |
| 상태           | PLANNED                                             |
| 선행 프로젝트  | 01~05                                               |
| 목표 태그      | `p06-operability-cloud`, `showcase-01-core-banking` |
| 주력 채용 신호 | 관측성, 장애 분석, runbook, 성능 실험, AWS 배포     |

## 문제 정의

정확한 기능도 느려짐·오류·backlog·batch 실패를 탐지하고 원인을 좁힐 수 없으면 운영하기 어렵습니다. 이 프로젝트는 원장·이체·이벤트·대사·보안 흐름에 일관된 telemetry를 넣고, 정해진 장애를 주입해 탐지·완화·복구 과정을 기록합니다. 같은 시스템을 AWS ECS/RDS에 임시 배포해 재현성과 비용을 확인합니다.

이 결과는 실제 사용자 운영이나 고가용성 증명이 아닙니다. `합성 workload를 사용한 프로덕션 유사 환경의 계획된 실험`으로만 설명합니다.

## 필수 범위

- structured log, metric, distributed trace와 correlation ID
- 핵심 SLI 정의: API latency·error, event lag·backlog, batch success·duration
- 합성 SLO와 alert threshold, 선택 근거
- Grafana dashboard 최대 3개
- runbook 최소 3개
- 의도적 장애 시나리오 3개
- 동일 조건 baseline·failure·recovery 측정
- Terraform 기반 AWS ECS/RDS 임시 배포
- health·readiness·graceful shutdown
- 거래·대사·감사 결과를 보는 최소 관리자 UI
- 비용 기록과 전체 cloud resource 제거

## 비범위

- 24시간 실제 on-call
- multi-region·고가용성 인증
- Kubernetes
- 실제 고객 트래픽
- auto-scaling 최적화
- `production-ready` 표현

## 관측 질문

1. 사용자가 느낀 이체 지연과 내부 DB·event 지연을 같은 correlation로 연결할 수 있는가?
2. broker 중단 때 거래 commit은 유지되고 backlog가 증가하는 것을 볼 수 있는가?
3. DB connection 고갈과 애플리케이션 오류를 구분할 수 있는가?
4. batch 실패가 마지막 성공 실행과 차이 건 수에 어떤 영향을 주는가?
5. 권한 거절 급증과 인증 실패를 구분할 수 있는가?

## 장애 시나리오

### A. Broker 일시 중단

- 예상: 거래 commit 유지, outbox backlog 증가, event lag alert
- 복구: broker 복원 뒤 publisher drain, 고유 side effect 대조

### B. DB connection pressure

- 예상: latency·timeout 증가, readiness 정책 확인
- 복구: 부하 감소·pool 정상화, 금전 불변식 재검증

### C. Batch worker 강제 종료

- 예상: job FAILED/STOPPED, 부분 chunk까지만 commit
- 복구: 재시작 뒤 중복 없는 report 생성

장애 종류를 실행 중 임의로 바꾸지 않고 사전 hypothesis와 성공·실패 기준을 기록합니다.

## 배포 구조

```text
Internet-restricted demo access
  → ALB
  → ECS application task
  → RDS PostgreSQL

Admin access
  → minimal UI / authenticated API
Observability
  → temporary collector + dashboard stack
```

broker를 AWS에 어떻게 배포할지는 비용 estimate 뒤 정합니다. 로컬과 다른 구현을 사용하면 topology 차이를 명시하고 직접 성능 비교를 금지합니다.

## 산출물

- SLI dictionary와 synthetic SLO
- OpenTelemetry instrumentation과 correlation 규칙
- dashboard 3개 이하와 alert
- runbook·incident template
- 부하·장애 실험 스크립트
- Terraform ECS/RDS와 제거 절차
- 최소 관리자 UI
- 비용·배포·장애 evidence

## 10일 작업 계획

| 일차 | 작업                             | 완료 기준                     |
| ---- | -------------------------------- | ----------------------------- |
| 1    | 사용자 흐름·SLI·장애 hypothesis  | 지표 정의·측정점 확정         |
| 2    | SLO·dashboard·AWS ADR            | 합성 SLO와 비범위 명시        |
| 3    | log·metric·trace instrumentation | correlation로 흐름 연결       |
| 4    | dashboard·alert                  | 3개 화면에 핵심 질문 대응     |
| 5    | runbook·health·shutdown          | 탐지→진단→완화→검증 절차      |
| 6    | workload·fault automation        | baseline과 failure 조건 고정  |
| 7    | Terraform·UI·AWS 배포            | 임시 URL과 합성 데이터만 노출 |
| 8    | 세 장애 실험·복구                | 원본 지표·로그·정합성 생성    |
| 9    | AWS 제거·비용·evidence           | 리소스 0과 최종 비용 확인     |
| 10   | Portfolio Review·Red Team        | PASS와 🔴 0건 또는 HOLD       |

## 테스트와 측정

- health와 readiness의 의존성 차이
- SIGTERM·graceful shutdown 중 in-flight 요청
- 동일 workload의 local baseline·AWS baseline은 각각 별도 보고
- broker stop·restore와 outbox drain
- DB connection pressure와 timeout
- batch worker termination·restart
- auth failure·forbidden metric 분리

측정 항목:

- API latency p50/p95/p99, throughput, 오류·거절 수
- outbox backlog, delivery lag, drain duration
- DB pool active·pending·timeout
- batch duration·restart point·difference 수
- 장애 탐지 시간, 완화 시작, 정상 기준 회복 시간
- AWS 배포·테스트 시간과 비용

## 필요한 증거

- SLI별 query와 단위·분모
- workload 명령, 장비·task·DB 사양, duration, warm-up
- dashboard 전체 시간 범위와 raw export
- 세 장애의 hypothesis·timeline·결과·불변식 재검증
- Terraform apply/destroy 결과와 AWS resource 확인
- 비용 조회 시각과 비용 범위
- 실제 사용자·on-call이 없다는 limitation

## 예상 면접 질문

1. readiness에 DB·broker를 모두 넣으면 어떤 장애 전파가 생길 수 있나요?
2. broker 중단 때 거래 자체를 막지 않은 이유는 무엇인가요?
3. 탐지 시간과 복구 시간을 어떤 사건을 기준으로 측정했나요?
4. p95 latency가 개선됐다고 말하려면 어떤 조건을 같게 유지해야 하나요?
5. 이 클라우드 데모가 실제 운영 경험과 다른 점은 무엇인가요?

## 주요 위험과 다음 회차 연결

- dashboard 캡처의 예쁜 모양보다 query 정의와 raw evidence를 우선합니다.
- alert가 울렸다는 사실을 빠른 복구 성과로 바꿔 쓰지 않습니다.
- AWS topology와 local topology가 다르면 수치를 직접 비교하지 않습니다.
- 프로젝트 07부터 결제·파트너 흐름에도 같은 observability 계약을 적용합니다.
