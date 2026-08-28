# AI-Native Financial Backend Lab

> 1~3년차 Java/Spring 금융 백엔드 개발자를 목표로, 하나의 금융 플랫폼을 6개월 동안 단계적으로 진화시키는 포트폴리오 계획입니다.

현재 상태는 **공통 하네스와 프로젝트 01 실행 골격 구현, 금융 기능 구현 전**입니다. `Money`, account migration·저장 smoke test는 하네스 검증용 최소 수직 절단이며 프로젝트 01의 journal·posting 완료를 의미하지 않습니다. 구현·측정·검증이 끝나지 않은 숫자와 기술 표현은 성과로 취급하지 않습니다.

## 이 포트폴리오가 증명하려는 것

- 돈의 보존과 데이터 정합성을 코드와 테스트로 지킬 수 있다.
- 동시성, 중복 요청, 비동기 전달, 외부 장애를 정상 흐름의 일부로 설계할 수 있다.
- 성능·장애·AI 품질을 측정 조건과 원본 근거까지 남길 수 있다.
- AI가 금융 핵심 로직을 직접 통제하지 않도록 권한과 사람의 승인 경계를 설계할 수 있다.
- 기술을 나열하지 않고 문제, 대안, 선택 근거, 비용, 남은 한계를 설명할 수 있다.

## 채용용 대표 사례

최종 포트폴리오에는 12개 회차를 모두 전면에 내세우지 않습니다. 아래 네 사례를 대표로 보여주고, 나머지는 진화 과정의 근거로 연결합니다.

1. **원장과 안전한 이체** — [복식부기 원장](projects/01-ledger-core/PLAN.md), [계좌이체와 동시성](projects/02-transfer-concurrency/PLAN.md)
2. **결제 신뢰성과 서비스 경계** — [결제 상태 머신](projects/07-payment-lifecycle/PLAN.md), [파트너 정산](projects/08-partner-settlement/PLAN.md), [서비스 분리 실험](projects/09-service-extraction/PLAN.md)
3. **근거가 제한된 AI 이상거래 탐지** — [AI FDS](projects/10-fraud-detection/PLAN.md)
4. **통제 가능한 금융 AI** — [금융 RAG](projects/11-financial-rag/PLAN.md), [책임 있는 Agent](projects/12-responsible-agent/PLAN.md)

## 6개월 흐름

```text
원장 → 동시성 이체 → 신뢰 가능한 이벤트 → 대사 배치 → 보안·감사
  → 관측성·1차 클라우드 → 결제 → 파트너 정산 → 서비스 분리 실험
  → AI FDS → 금융 RAG → 승인 기반 Agent → 최종 감사·2차 클라우드
```

- 기간: 2026-08-31 ~ 2027-02-28
- 개발: 12개 프로젝트 × 2주
- 최종 감사: 2주
- 상세 일정: [MASTER_PLAN.md](MASTER_PLAN.md)
- 기술 기준: [TECH_BASELINE.md](docs/TECH_BASELINE.md)
- 아키텍처 진화: [ARCHITECTURE_EVOLUTION.md](docs/ARCHITECTURE_EVOLUTION.md)
- 증거 규칙: [EVIDENCE_POLICY.md](docs/EVIDENCE_POLICY.md)
- 종료 평가: [EVALUATION_GATES.md](docs/EVALUATION_GATES.md)

## 하네스 실행

### 선행조건

- Git
- Java 17 이상으로 Gradle wrapper를 시작할 수 있는 환경
- Full 검증과 로컬 DB 실행에는 Docker daemon

프로젝트 compile과 test는 Foojay resolver가 제공하는 Eclipse Temurin Java 21 toolchain을 사용합니다. 최초 toolchain·dependency 다운로드에는 network가 필요할 수 있습니다.

### 공개 명령

```bash
./gradlew harnessDoctor
./gradlew harnessFast
./gradlew harnessFull
./gradlew knowledgeCheck
./gradlew knowledgeExport
./gradlew promoteHarnessEvidence -PtargetProject=01-ledger-core
```

- `harnessDoctor`: toolchain, wrapper, 공급망 metadata, Docker 상태 진단
- `harnessFast`: format, strict compile, unit, architecture, knowledge 검증
- `harnessFull`: Fast와 PostgreSQL 18.6·Flyway·JPA·Boot·packaging 검증
- `knowledgeExport`: `build/knowledge/knowledge.jsonl` 생성
- `promoteHarnessEvidence`: clean HEAD의 성공한 Full manifest를 지정 프로젝트 evidence로 명시적으로 승격

Windows에서는 `./gradlew` 대신 `gradlew.bat`를 사용합니다. Docker가 꺼져 있어도 Fast는 실행 가능하며 Full은 container test 전에 조치가 포함된 오류로 실패합니다.

로컬 PostgreSQL은 다음 명령으로 관리합니다.

```bash
./gradlew localUp
./gradlew localDown
```

개발 volume까지 삭제하는 `localReset`은 명시적으로 실행할 때만 사용합니다. 상세 계약과 완료 조건은 [하네스 엔지니어링 구현 계획](docs/HARNESS_ENGINEERING_PLAN.md)을 따릅니다.

## 증거 우선 원칙

공개 문서의 핵심 주장에는 다음 라벨을 사용합니다.

- `[검증됨]`: 재현 가능한 명령, commit SHA, 환경, 원본 결과가 연결됨
- `[가정]`: 아직 실험 전인 설계 가설
- `[불명]`: 현재 자료로 확인할 수 없음
- `[모순]`: 서로 다른 근거가 충돌함. 공개 성과로 사용 금지

합성 데이터와 개인 장비 부하는 실제 금융 트래픽이나 운영 경험으로 표현하지 않습니다. 임시 AWS 배포는 `클라우드 데모` 또는 `프로덕션 유사 환경`으로만 표기합니다.

## 프로젝트 종료 규칙

각 프로젝트 마지막 날에는 다음 순서로 평가합니다.

1. 자동화 테스트와 증거 묶음 확인
2. `hiring-sim-portfolio-review`로 PASS/REJECT 판정
3. `hiring-sim-portfolio-redteam`으로 수치·서사·용어를 적대적으로 검증
4. 🔴 High 0건일 때만 `RELEASED`와 Git 태그 부여

현재 계획 자체의 평가는 [포트폴리오 리뷰](reviews/PLAN_PORTFOLIO_REVIEW.md)와 [레드팀 사전 분석](reviews/PLAN_REDTEAM_REVIEW.md)에 기록했습니다.

## 범위와 면책

- 모든 계좌·거래·고객 데이터는 합성 데이터입니다.
- 실제 금융서비스, 투자 자문, 신용심사 또는 규제 인증을 제공하지 않습니다.
- 개인 프로젝트이므로 팀 성과나 실사용자 임팩트를 주장하지 않습니다.
- 구현되지 않은 기술은 계획 스택일 뿐 사용 경험으로 기재하지 않습니다.
