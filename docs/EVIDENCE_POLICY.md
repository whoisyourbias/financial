# 증거·수치·주장 관리 정책

## 목적

포트폴리오의 신뢰도는 좋은 숫자의 크기가 아니라 숫자가 무엇을 의미하며 어떻게 재현되는지에 달려 있습니다. 이 문서는 계획, 측정, 검증, 공개 주장 사이의 승격 규칙을 정의합니다.

## 주장 라벨

| 라벨       | 의미                           | 공개 대표 성과 사용         |
| ---------- | ------------------------------ | --------------------------- |
| `[가정]`   | 구현·실험 전의 가설 또는 기대  | 금지                        |
| `[불명]`   | 자료가 없거나 현재 확인 불가능 | 금지                        |
| `[검증됨]` | 원본 증거와 재현 절차로 확인   | 허용                        |
| `[모순]`   | 문서·대시보드·원본이 충돌      | 금지, 수정 전 RELEASED 불가 |

계획 문서의 미래형 목표는 기본적으로 `[가정]`입니다. 체크박스가 완료됐다는 사실만으로 `[검증됨]`이 되지 않습니다.

## 프로젝트 증거 구조

구현 단계에서 각 프로젝트는 다음 구조를 사용합니다.

```text
projects/<id>/evidence/
├── MANIFEST.md
├── HARNESS_EVIDENCE.md
├── environment.md
├── commands.md
├── raw/
├── dashboards/
├── diagrams/
├── adr/
├── incidents/
└── results.md
```

`MANIFEST.md`는 구현·실험 결과가 생긴 뒤에만 만드는 프로젝트 증거의 권위 manifest이며, 이 문서가 공통 schema를 정의합니다. `HARNESS_EVIDENCE.md`는 선택적인 공통 하네스 실행 기록일 뿐 프로젝트 기능 완료나 `EVIDENCE_READY`를 증명하지 않습니다. 공통 harness의 `evidenceCheck`가 `MANIFEST.md`를 검사하며, 해당 명령이 실패하면 프로젝트는 `EVIDENCE_READY`로 이동할 수 없습니다.

파일은 다음 YAML front matter로 시작합니다. `schemaVersion`은 정수, `projectId`는 실제 디렉터리 ID, `sourceSha`는 40자리 commit SHA, `generatedAt`은 UTC ISO-8601, `intendedTags`는 하나 이상의 고유 문자열 목록입니다.

```yaml
---
schemaVersion: 1
projectId: 01-ledger-core
sourceSha: 0123456789abcdef0123456789abcdef01234567
generatedAt: 2026-09-01T00:00:00Z
intendedTags:
  - p01-ledger-core
environment:
  os: macOS-15.6
  cpu: Apple-M3
  memoryBytes: 17179869184
  jvm: temurin-21.0.8
  dependencies: [postgresql-18.6]
commands:
  - command: ./gradlew harnessFull
    configPaths: [gradle/libs.versions.toml]
dataset:
  seed: "42"
  size: 1000
  distribution: fixed-fixture-v1
  version: "1"
datasetNotApplicableReason: null
aiContext: null
aiNotApplicableReason: "non-AI project"
measurement:
  warmupSeconds: 30
  durationSeconds: 300
  concurrency: 8
  requestMix: fixed-mix-v1
measurementNotApplicableReason: null
artifacts:
  - path: raw/result.json
    sha256: 0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
    summaryRef: results.md#summary
limitations:
  - synthetic data only
---
```

AI 프로젝트와 final audit는 위 두 AI 필드를 다음 object로 교체합니다.

```yaml
aiContext:
  provider: local
  model: example-model
  modelVersion: "1"
  promptSha256: null
  promptNotApplicableReason: "non-generative model"
  corpusVersion: synthetic-dataset-v1
  corpusNotApplicableReason: null
aiNotApplicableReason: null
```

front matter의 key와 type은 위 예시로 고정합니다. `schemaVersion`, `memoryBytes`, dataset `size`, measurement 숫자는 integer, 나머지 scalar는 string 또는 명시된 null, `intendedTags`·`dependencies`·`commands`·`configPaths`·`artifacts`·`limitations`는 목록입니다. AI 프로젝트 10~12와 `final-audit`의 `aiContext`는 non-null object이며 `provider`, `model`, `modelVersion`, `promptSha256`, `promptNotApplicableReason`, `corpusVersion`, `corpusNotApplicableReason` key를 모두 가집니다. SHA 필드는 non-null이면 64자리 lowercase hex이고, prompt나 corpus가 적용되지 않으면 값은 null, 대응 reason은 non-empty string이어야 합니다. 비AI 프로젝트만 `aiContext: null`과 non-empty `aiNotApplicableReason`을 허용합니다. `dataset`, `measurement`가 null일 때도 대응하는 `*NotApplicableReason`은 비어 있지 않아야 합니다. front matter 뒤에는 `Environment`, `Commands`, `Dataset`, `AI Context`, `Artifacts`, `Limitations` H2가 각각 정확히 한 번 있고 front matter를 사람이 검토할 설명과 링크를 제공합니다.

`MANIFEST.md`에는 다음을 빠짐없이 기록합니다.

- 프로젝트 ID와 결과 생성 시각
- 검증 대상 구현의 `sourceSha`와 `intendedTags`; evidence commit 자체 SHA는 자기참조로 본문에 넣지 않음
- OS, CPU, memory, JVM, container·DB·broker 버전
- 실행 명령과 설정 파일 경로
- 데이터셋 seed·크기·분포·버전
- AI provider·model·prompt·document corpus 버전
- warm-up, duration, concurrency, request mix
- raw 파일과 요약 표의 연결
- known limitation과 재현 실패 조건

계획 단계에는 빈 `MANIFEST.md`나 결과 파일을 만들지 않습니다. 공통 하네스 검증 기록만 먼저 보존해야 한다면 `promoteHarnessEvidence`로 `HARNESS_EVIDENCE.md`와 `raw/harness/**`를 만들고 프로젝트 증거와 구분합니다. 구현 증거가 생기면 프로젝트의 `projects/<id>/evidence/MANIFEST.md`를 생성·commit하고 깨끗한 candidate worktree에서 `./gradlew evidenceCheck -PtargetProject=<id>`로 검증합니다. tag 생성 전 manifest의 `intendedTags`는 필수지만 실제 tag는 아직 없어야 합니다.

`evidenceCheck`는 YAML key·type, 필수 H2, repository-relative path, 파일 존재와 SHA-256, summary heading, `sourceSha`의 Git 조상 관계, `sourceSha..release-candidate`의 변경 경로를 검사합니다. 허용 경로는 `projects/**/evidence/**`, 대상 프로젝트의 `RESULTS.md`·`PORTFOLIO_REVIEW.md`·`REDTEAM_REVIEW.md`, 루트 `README.md`, `reviews/**`뿐입니다. 코드·빌드·설정·migration·`contracts/**`·계약 매트릭스·OpenAPI를 포함해 allowlist 밖의 파일이 하나라도 바뀌면 기존 evidence를 무효화하고 변경 commit을 새 `sourceSha`로 전체 필수 검증을 다시 실행합니다.

Portfolio Review와 Red Team을 통과한 evidence commit에 각 annotated tag를 만든 뒤, tag tree에 clean manifest가 있고 manifest의 `sourceSha`가 tag commit의 도달 가능한 조상이며 중간 diff가 allowlist만 포함하고 artifact가 그 source를 증명하는지 확인해야 `RELEASED`가 됩니다. 프로젝트 06처럼 프로젝트 tag와 showcase tag가 같은 candidate를 가리키면 두 이름을 `intendedTags`에 모두 기록합니다.

최종 통합 쇼케이스는 `reviews/evidence/MANIFEST.md`를 aggregate manifest로 사용합니다. 같은 schema에서 `projectId: final-audit`, `intendedTags: [showcase-02-ai-payment-ops]`로 두고, `artifacts`가 12개 프로젝트 manifest checksum과 통합 회귀·AI 평가·cloud 제거 결과를 참조해야 합니다. `./gradlew evidenceCheck -PtargetProject=final-audit`는 이 특별 경로와 12개 하위 manifest 연결을 검사합니다.

## 리뷰 attestation commit

심사할 공개 payload를 commit `C0`으로 동결한 뒤 두 스킬을 `C0`에 실행합니다. 프로젝트 심사는 `projects/<id>/PORTFOLIO_REVIEW.md`와 `projects/<id>/REDTEAM_REVIEW.md`, 최종 감사는 `reviews/FINAL_PORTFOLIO_REVIEW.md`와 `reviews/FINAL_REDTEAM_REVIEW.md`만 추가한 직계 후속 commit `C1`을 만듭니다. 이 `C1`이 `REVIEWED`와 `RELEASE_CANDIDATE` 대상이며 `C0..C1`에는 해당 두 파일 이외의 tracked 변경이 없어야 합니다.

두 리뷰 파일은 다음 front matter를 가집니다.

```yaml
---
schemaVersion: 1
reviewedSha: 0123456789abcdef0123456789abcdef01234567
skill: hiring-sim-portfolio-review
generatedAt: 2026-09-01T00:00:00Z
verdict: PASS
highCount: 0
---
```

`reviewedSha`는 둘 다 `C0`, `verdict`는 Portfolio Review에서 `PASS`, Red Team에서는 `HIGH_0`이어야 합니다. Red Team의 `highCount`는 0이어야 하고 Portfolio Review의 적용되지 않는 count도 0으로 기록합니다. 보고서 생성 뒤 payload를 바꾸면 새 `C0`에서 두 심사를 모두 다시 실행합니다. tag는 검증된 attestation commit `C1`을 가리키며, 이때 “같은 commit 심사”는 같은 `reviewedSha` payload를 두 보고서가 증명한다는 뜻입니다.

## 수치 기록 계약

모든 지표 표에는 최소한 다음 열을 둡니다.

| 필드 | 설명                                             |
| ---- | ------------------------------------------------ |
| 이름 | 같은 지표에 문서마다 다른 이름을 쓰지 않음       |
| 정의 | 측정 시작·종료 지점과 포함·제외 시간             |
| 단위 | ms, s, requests/s, 건 등                         |
| 집계 | 평균, 중앙값, p95, p99, 합계, 최대값 중 무엇인지 |
| 분모 | 요청 수, 이벤트 수, 평가 문항 수 등              |
| 환경 | 로컬·CI·AWS와 주요 자원                          |
| 기간 | warm-up과 본 측정 시간                           |
| 원본 | raw 결과 또는 dashboard query 링크               |

### 성능 표현 규칙

- enqueue 응답시간을 전체 업무 완료시간으로 표현하지 않습니다.
- 평균과 p95·p99를 바꿔 쓰지 않습니다.
- 최대 순간값을 지속 처리량으로 표현하지 않습니다.
- 오류 응답을 제외했다면 제외 기준과 개수를 기록합니다.
- 단일 개인 장비 결과에는 `해당 환경의 capacity experiment`라고 씁니다.
- 비교 실험은 데이터, 요청 비율, 장비, duration을 동일하게 유지합니다.
- 여러 변경을 동시에 적용했다면 단일 기술의 인과 성과로 귀속하지 않습니다.

## 금융 정합성 증거

- 원장: 거래별 차변 합계와 대변 합계
- 결제 명령: 승인 금액, 누적 취소액, 잔액, 멱등키 결과, 원장 posting 합계
- 웹훅: 발행 의도 수, HTTP 시도 수, 성공 수, 고유 event 수, 상점 중복 억제 수
- 가상계좌 대사: 합성 은행 입금 합계, ledger 합계, 차이 건수, 해결 상태
- 정산: gross, cancel, fee, tax, net의 거래별·상점별 합계

합계가 맞더라도 개별 거래가 잘못 매핑될 수 있으므로 합계와 개별 식별자 대사를 함께 확인합니다.

## AI 평가 증거

### FDS

- 합성 데이터 생성 규칙과 seed를 공개합니다.
- train/validation/test 분리를 고정하고 test를 튜닝에 재사용하지 않습니다.
- class 분포와 confusion matrix 원본을 보관합니다.
- precision, recall, FPR은 각각 분모를 기록합니다.
- 규칙 기준선과 모델을 같은 test set에서 비교합니다.
- 실제 금융사기 탐지 성능으로 일반화하지 않습니다.

### RAG

- retrieval Recall@K와 최종 답변의 근거 일치를 분리합니다.
- 답이 있는 질문, 답이 없는 질문, 적대적 질문을 구분합니다.
- LLM evaluator 결과만 사용하지 않고 사람이 확인한 고정 표본을 둡니다.
- 인용 문서 ID와 실제 문장 위치를 보관합니다.
- 문서 corpus가 바뀌면 과거 결과와 직접 비교하지 않거나 동일 corpus로 재실행합니다.

### Agent

- 정상 도구 호출, 권한 없는 요청, prompt injection, 오래된 승인, 중복 승인을 분리합니다.
- `무단 변경 0건`은 명시한 공격 corpus와 테스트 실행 범위에서만 주장합니다.
- 모델 응답이 아니라 영속 상태와 audit record로 실행 여부를 확인합니다.

## 이미지·다이어그램 정책

- dashboard 캡처에는 시간 범위, query, 단위, 범례가 보여야 합니다.
- 문서 표의 숫자는 캡처 또는 raw query 결과와 대조합니다.
- 이미지 보정으로 축·수치·경고를 가리지 않습니다.
- 아키텍처 다이어그램의 기술과 실행 단위를 실제 코드·배포에서 확인합니다.
- 계획 구성요소와 구현 구성요소를 같은 모양으로 섞지 않습니다.
- 확인하지 못한 이미지는 `미확인`으로 기록합니다.

## 공개 성과 승격 절차

1. raw evidence 생성
2. `results.md`에 조건과 함께 요약
3. 포트폴리오 리뷰에서 정합성 대조
4. 레드팀에서 측정 바꿔치기·용어·인과 검사
5. 🔴 0건이면 `[검증됨]` 부여
6. 대표 README에는 맥락과 한계를 포함해 한 번만 기재

README, 결과표, dashboard가 같은 수치를 반복할 때는 한 원본으로부터 생성됐음을 링크로 보여줍니다.

## 금지 주장

- 근거 없는 생산성 배수 또는 시간 절감률
- 실사용자가 없는데 사용자 만족·비즈니스 성과를 수치화한 표현
- AWS에 한 번 배포한 것을 운영 경험으로 표현
- 합성 트래픽을 실제 대규모 트래픽으로 표현
- FDS·RAG·Agent 실험을 금융 의사결정 자동화 성과로 표현
- 기능 추가와 인덱스 변경처럼 여러 원인을 하나의 기술 효과로 귀속
- 검토하지 않은 대안을 비교했다고 서술

## 보존과 삭제

- 공개 가능한 합성 raw evidence는 Git 또는 release artifact에 보관합니다.
- 크기가 큰 결과는 checksum과 생성 명령을 Git에 남기고 외부 artifact 위치를 기록합니다.
- API key와 cloud credential이 포함된 결과는 즉시 폐기하고 재발급합니다.
- AWS evidence를 수집한 뒤 리소스를 제거하고 제거 시각·최종 비용을 기록합니다.
