# 하네스 엔지니어링 구현 계획

## 상태와 목적

| 항목 | 값 |
| --- | --- |
| 상태 | APPROVED |
| 구현 상태 | IMPLEMENTED_LOCAL_VERIFIED (CI_PENDING) |
| 기준일 | 2026-08-28 |
| 적용 범위 | 저장소 공통 실행 하네스, 프로젝트 01 실행 골격, 후속 지식 하네스 |
| 기본 원칙 | 에이전트 중립형, 로컬 우선, Fast/Full 2단계 검증, 명시적 증거 승격 |

이 계획은 에이전트와 사람이 금융 백엔드 코드를 같은 명령으로 이해·수정·검증하도록 저장소가 소유하는 실행 규약을 정의합니다. Codex 전용 설정은 공통 명령과 지식 문서를 연결하는 얇은 어댑터로 제한합니다.

`IMPLEMENTED_LOCAL_VERIFIED`는 macOS arm64 로컬 환경에서 Doctor/Fast/Full과 부정 경로를 확인했다는 뜻입니다. GitHub Actions의 Linux·Windows 결과와 branch protection 설정은 원격 저장소에서 확인하기 전까지 `CI_PENDING`입니다.

## 0. 단계와 범위 경계

서로 다른 실패 원인과 변경 주기를 가진 작업을 하나의 완료 게이트로 묶지 않습니다.

| 단계 | 목적 | 프로젝트 01 시작 차단 | 주요 산출물 |
| --- | --- | --- | --- |
| A. Core Harness MVP | 빌드·모듈·DB smoke test와 공통 검증 명령 확립 | 예 | Gradle wrapper, 모듈, Doctor/Fast/Full, Compose, 최소 CI, 실행 manifest |
| B. Knowledge Harness | 검색·RAG 입력용 문서 catalog와 결정론적 export | 아니요 | catalog, sources, `knowledgeCheck`, `knowledgeExport`, golden fixture |

단계 A가 완료되면 프로젝트 01의 journal·posting 구현을 시작할 수 있습니다. 단계 B는 별도 commit과 별도 검증으로 진행하며, 미완료여도 프로젝트 01 기능 게이트를 막지 않습니다. 단계 B가 통합된 뒤에는 `knowledgeCheck`를 `harnessFast`에 편입합니다.

### 단계 A 포함

- 저장소·하위 디렉터리의 에이전트 지침 계층
- Java·Gradle 멀티모듈 골격과 의존 경계
- 프로젝트 01의 `Money`, account migration, JPA 저장·조회 smoke slice
- 로컬 PostgreSQL Compose와 격리된 Testcontainers 검증
- Doctor/Fast/Full 명령, 실행 manifest, 명시적 evidence 승격
- Linux·Windows Fast와 Linux Full을 실행하는 최소 GitHub Actions

### 단계 A 비범위

- journal, posting, HTTP API, 인증, outbox와 실제 원장 기능
- AWS, 배포 파이프라인, 고가용성, 운영 secret
- vector database, embedding, LLM 호출
- 실제 사용자 데이터와 외부 금융기관 연동

프론트엔드는 프로젝트 06에서 구현합니다. 단계 A에는 미래 API·이벤트 계약의 소유권 문서와 프론트엔드 지침만 만들고 application이나 Node dependency는 생성하지 않습니다.

## 1. 저장소·에이전트 하네스

### 지침 계층

- 루트 `AGENTS.md`에 프로젝트 목표, 필수 문서 읽기 순서, 금융 불변식, 변경 절차, 증거 정책, 금지 행위, 공통 검증 명령을 둡니다.
- `platform/AGENTS.md`에는 Java·Spring·모듈 경계·transaction·migration 규칙을 둡니다.
- `frontend/AGENTS.md`에는 API 소비, 오류 처리, 접근성, browser test 규칙을 둡니다.
- `docs/domain/AGENTS.md`에는 지식 문서의 출처, 상태, 변경·검토 규칙을 둡니다.
- 가까운 디렉터리의 지침은 루트 규칙을 구체화할 수 있지만 금융 불변식, 증거 정책, 보안 경계를 완화할 수 없습니다.

### Codex 연결

- `.codex/config.toml`에는 `project_doc_max_bytes = 65536`만 설정합니다.
- 모델, provider, sandbox, 승인 정책은 저장소에서 강제하지 않습니다.
- lifecycle hook은 추가하지 않습니다. 결정론적 검사는 Gradle task와 저장소 스크립트로 제공해 다른 에이전트와 사람도 같은 경로를 사용합니다.
- 새 Codex session에서 활성화된 루트·하위 지침과 공통 명령을 요약하게 해 instruction discovery를 확인합니다.

## 2. 공개 명령과 실패 계약

Java를 실행할 수 있는 환경에서는 Windows에서 `gradlew.bat`, 다른 환경에서 `./gradlew`를 사용합니다. Java 자체가 없으면 wrapper도 실행할 수 있으므로 README에 Java 21 설치와 확인 절차를 별도 bootstrap 단계로 둡니다.

| 명령 | 단계 | 역할 | Docker daemon 필요 |
| --- | --- | --- | --- |
| `harnessDoctor` | A | toolchain, wrapper, Docker 상태, 필수 파일, dependency cache 진단 | 아니요 |
| `harnessFast` | A | format, compile, unit, architecture, 정적 검사 | 아니요 |
| `harnessFull` | A | Fast, migration, PostgreSQL integration, Boot context, packaging | 예 |
| `localUp` | A | Compose PostgreSQL 시작과 health 대기 | 예 |
| `localDown` | A | Compose service 중지, volume 보존 | 예 |
| `localReset` | A | Compose service와 개발 volume 삭제 | 예, 명시적 실행만 |
| `promoteHarnessEvidence` | A | 성공한 실행 결과를 프로젝트 evidence로 승격 | 실행 결과에 따름 |
| `knowledgeCheck` | B | metadata, 내부 링크, 출처, evidence, 중복 ID 검증 | 아니요 |
| `knowledgeExport` | B | 검색·RAG 입력용 JSONL 생성 | 아니요 |

`localReset`은 파괴적 명령이므로 다른 task의 dependency로 연결하지 않고 실행 전 대상 Compose project와 삭제할 volume을 출력합니다.

### Task 의존 관계

```text
formatCheck + staticAnalysis + compile + unitTest + architectureTest
  → harnessFast

harnessFast + migrationFreshTest + migrationUpgradeTest
  + postgresIntegrationTest + bootContextTest + packaging
  → harnessFull

knowledgeCheck
  → knowledgeExport

successful harnessFull manifest
  → explicit promoteHarnessEvidence
```

단계 B가 통합된 뒤의 `harnessFast`는 `knowledgeCheck`도 포함합니다. `knowledgeExport`는 생성 task이므로 Fast에 포함하지 않습니다.

- `formatCheck`는 Spotless formatter check로 구현하고 plugin과 formatter engine 버전을 version catalog에 고정합니다.
- `staticAnalysis`는 Java compiler `-Xlint:all -Werror`와 저장소 Checkstyle 규칙을 실행합니다.
- 생성 코드와 `build/`는 format·static-analysis 입력에서 제외하고, 새 warning은 baseline으로 숨기지 않고 실패시킵니다.

### 실패·네트워크 규약

- 모든 공개 검증 명령은 성공 시 exit code 0, 검증 실패나 필수 선행조건 누락 시 non-zero로 종료합니다.
- `harnessDoctor`에서 Docker CLI 부재나 daemon 정지는 경고로 보고하고 Fast 사용 가능 여부를 계속 진단합니다.
- `harnessFull`은 Docker가 없거나 정지된 경우 container test를 시작하기 전에 원인과 복구 명령을 출력하고 실패합니다.
- 최초 dependency와 toolchain 해석에는 network가 필요할 수 있음을 문서화합니다. cache가 준비된 환경에서는 `./gradlew --offline harnessFast`도 성공해야 합니다.
- 외부 URL의 현재 상태는 Fast에서 조회하지 않습니다. 변경 가능한 외부 출처는 사용할 때 별도로 재검증합니다.
- 일반 검증은 `build/` 밖의 추적 파일을 만들거나 수정하지 않습니다.
- task별 timeout과 container startup timeout을 고정하고, timeout은 원인 없는 일반 test failure와 구분해 보고합니다.
- wrapper 시작·Gradle configuration 전에 발생한 bootstrap 실패는 manifest를 만들 수 없으므로 console과 CI job log를 원본으로 보존합니다.

## 3. 버전·공급망·재현성 계약

### 고정 기준

| 영역 | 고정 버전·방식 |
| --- | --- |
| JVM | Eclipse Temurin Java 21 toolchain |
| Build | Gradle Wrapper 9.7.1, Kotlin DSL |
| Application | Spring Boot 4.1.1 |
| Database | PostgreSQL 18.6 |
| Persistence | Spring Data JPA |
| Schema | Flyway only |
| Test | JUnit Jupiter 6.0.3, Testcontainers 2.0.5, ArchUnit 1.5.0 |

Spring Boot BOM이 관리하는 library version은 개별 선언하지 않습니다. BOM 밖의 plugin과 도구 버전은 명시적으로 고정하고, 버전 변경은 기능 변경과 분리합니다.

### 공급망 방어

- wrapper의 distribution URL과 `distributionSha256Sum`을 추적하고 wrapper JAR checksum을 공식 값과 대조합니다.
- 모든 configuration에 dependency locking을 적용하고 `gradle/verification-metadata.xml`과 저장소 keyring에서 artifact와 plugin checksum·서명을 검증합니다. 서명이 없거나 공개키 서버에서 키를 받지 못한 생성기 명시 항목은 고정 SHA-256으로 검증합니다.
- dynamic version, version range, `SNAPSHOT`, `mavenLocal()`을 금지합니다.
- repository와 plugin repository를 `settings.gradle.kts`에 중앙화하고 허용하지 않은 project-level repository를 실패시킵니다.
- PostgreSQL image는 사람이 읽는 tag와 multi-architecture manifest digest를 함께 고정합니다. 실행 manifest에는 실제 architecture와 resolved image digest를 기록합니다.
- GitHub Actions는 version tag가 아니라 검토한 commit SHA로 고정하며 기본 권한은 `contents: read`만 허용합니다.
- Fast와 Full은 cloud credential, repository write token, 실제 secret을 요구하지 않습니다.

### OS 간 결정론

- source와 generated text는 UTF-8, Unicode NFC, LF를 기준으로 처리합니다.
- hash와 manifest의 파일 경로는 repository-relative POSIX 형식으로 기록합니다.
- 시간은 UTC ISO-8601로 기록하되, 실행 시각은 deterministic export의 입력이나 checksum에 포함하지 않습니다.
- JSON과 JSONL은 key 순서, 배열 순서, 숫자 표현, 마지막 newline을 고정합니다.
- locale과 timezone에 영향을 받는 test는 `Locale.ROOT`와 UTC를 명시합니다.
- 무작위·생성형 test는 seed와 case 수를 입력으로 받고 manifest에 기록합니다.
- 실행 manifest는 시각과 환경을 포함하므로 byte-for-byte 동일성을 요구하지 않습니다. 대신 동일 commit·입력의 `inputsDigest`와 deterministic artifact checksum이 같아야 합니다.

## 4. 로컬 실행과 프로젝트 01 골격

### Compose와 Testcontainers의 역할

| 방식 | 목적 | 데이터 수명 | 검증 사용 |
| --- | --- | --- | --- |
| Docker Compose | 개발자가 로컬 PostgreSQL 의존성을 한 명령으로 재현 | `localDown`에서 보존 | 수동 개발·확인 |
| Testcontainers | test마다 격리된 PostgreSQL에서 migration과 JPA 검증 | test 종료 시 제거 | `harnessFull` |

Compose와 Testcontainers는 같은 PostgreSQL tag·digest, database name, encoding, timezone 정책을 사용합니다. Testcontainers reuse는 비활성화하고 test 간 상태 공유를 금지합니다. Compose healthcheck와 Testcontainers wait strategy는 같은 readiness 조건을 사용합니다.

### 모듈 구조

```text
platform/
├── bootstrap
├── ledger
└── shared-kernel
tools/
└── knowledge-harness  # 단계 B
```

- package root는 `dev.whoisyourbias.financial`을 사용합니다.
- 의존 방향은 `bootstrap → ledger → shared-kernel`만 허용합니다.
- `shared-kernel`에는 `Money`, identifier, time abstraction, 공통 오류 계약만 허용합니다.
- `ledger`의 JPA entity와 repository는 다른 모듈에 공개하지 않습니다.
- `bootstrap`은 조립과 application 시작만 담당하며 domain rule을 소유하지 않습니다.
- Gradle project dependency와 `api`/`implementation` 노출을 우선 방어선으로 사용하고 ArchUnit을 2차 방어선으로 둡니다.
- Gradle TestKit functional test에서 역방향 dependency와 entity·repository 직접 접근이 실제로 실패함을 검증합니다.

### 첫 데이터베이스 smoke slice

- `Money(amountMinor, currency)`는 signed value를 표현할 수 있게 두고 currency 불일치와 `Math.*Exact` overflow를 실패시킵니다. posting 금액의 양수 제약은 프로젝트 01의 posting 도메인 경계에서 적용합니다.
- 첫 Flyway migration은 `ledger_account` table만 생성합니다.
- account에는 UUID, account type, ISO-4217 currency, 생성 시각을 저장하며 balance column은 두지 않습니다.
- JPA는 `ddl-auto=validate`와 Open EntityManager in View 비활성화를 기본값으로 사용합니다.
- Fresh 시나리오는 빈 PostgreSQL에 전체 migration을 적용하고 schema validation, account 저장·조회, Boot context 시작을 확인합니다.
- Upgrade 시나리오는 지원하는 직전 migration 상태에서 현재 head까지 순차 적용합니다. migration이 하나뿐이면 `NOT_APPLICABLE: no previous migration`을 manifest에 기록하며 성공으로 위장하지 않습니다.
- Flyway `repair`나 Hibernate schema generation으로 migration 오류를 우회하지 않습니다.
- journal, posting, HTTP API, 인증, outbox는 이 작업에서 구현하지 않습니다.

## 5. 실행 manifest와 evidence 승격

### 임시 실행 결과

모든 Fast와 Full 실행은 `build/reports/harness/<run-id>/manifest.json`을 생성합니다. 실패한 실행도 가능한 범위의 진단과 완료된 task 결과를 남기되 `status: FAILED`로 표시합니다.

manifest에는 다음 필드를 포함합니다.

- schema version, run ID, command, PASS/FAILED, 시작·종료 UTC
- Git commit SHA, branch, dirty 여부
- OS, architecture, CPU count, memory, locale, timezone
- Java vendor·version, Gradle version
- Docker client·server version과 container image tag·resolved digest
- task별 outcome과 test count·실패 수·report 경로
- migration 시작·종료 version과 Fresh/Upgrade 결과
- 생성형 test seed·case 수
- knowledge catalog checksum과 export checksum(단계 B 통합 뒤)
- 입력 파일 목록의 checksum으로 계산한 `inputsDigest`
- artifact의 repository-relative path, SHA-256, size
- warning, skipped reason, known limitation

환경 변수의 값, token, password, connection string, 사용자 홈 절대 경로는 manifest나 raw log에 기록하지 않습니다.

### 명시적 승격

`promoteHarnessEvidence`는 Fast나 Full의 dependency가 아니며 다음 조건을 모두 만족할 때만 동작합니다.

1. source worktree가 명령 시작 시 깨끗합니다.
2. source manifest가 현재 HEAD의 성공한 `harnessFull` 결과입니다.
3. manifest가 참조하는 artifact checksum이 모두 일치합니다.
4. secret pattern 검사에서 노출이 없습니다.
5. `-PtargetProject=<id>`가 실제 `projects/<id>`와 일치합니다.

명령은 raw JSON manifest와 필요한 report를 `projects/<id>/evidence/raw/harness/<source-sha>/`에 복사하고 `projects/<id>/evidence/MANIFEST.md`에 명령·환경·checksum·한계를 연결합니다. 같은 source SHA와 checksum의 재실행은 변경이 없는 idempotent 작업이어야 하며, 같은 SHA에 다른 결과가 있으면 덮어쓰지 않고 실패합니다.

승격 후 생긴 추적 파일은 별도 evidence commit으로 검토합니다. `MANIFEST.md`에는 검증 대상인 source SHA를 기록하고, evidence commit 자체는 Git 이력과 tag·PR에서 식별합니다. commit이 자기 SHA를 본문에 기록하는 자기참조 계약은 두지 않습니다.

## 6. 단계 B — 도메인 지식과 검색·RAG 준비

### 지식 catalog

기존 문서 내용을 복제하지 않고 `docs/domain/catalog.yaml`에서 검색 대상과 metadata를 관리합니다.

| 필드 | 계약 |
| --- | --- |
| `id` | 저장소 전체에서 유일하고 변경하지 않는 문서 ID |
| `title` | 사람이 읽는 문서 제목 |
| `path` | 저장소 상대 경로 |
| `kind` | `policy`, `architecture`, `domain`, `plan`, `exit-criteria`, `review`, `external` |
| `domains` | ledger, transfer, payment, security, operations, FDS, RAG, agent 등의 목록 |
| `status` | `planned`, `verified`, `unknown`, `contradicted` 중 하나 |
| `version` | 문서 의미 버전 |
| `reviewedAt` | 마지막 검토일 |
| `appliesTo` | 적용 프로젝트·모듈 목록 |
| `sourceRefs` | `sources.yaml`의 공식 외부 출처 ID 목록 |
| `evidenceRefs` | 저장소 내부 evidence manifest·artifact 참조 목록 |

`status`는 기존 `[가정]`, `[검증됨]`, `[불명]`, `[모순]` 정책과 각각 대응합니다. `verified`는 재현 가능한 `evidenceRefs` 또는 등록된 공식 `sourceRefs` 없이 사용할 수 없습니다. 존재만 하는 URL이나 체크박스는 검증 근거가 아닙니다.

### 외부 출처

`docs/domain/sources.yaml`에는 다음 정보만 저장합니다.

- 고유 출처 ID
- 공식 문서 제목과 URL
- 발행 기관
- 마지막 확인일
- 적용 도메인과 사용 범위
- 원문을 저장하지 않는다는 표시

변경 가능성이 있는 외부 자료는 사용할 때 원문을 다시 확인합니다. Fast는 외부 URL의 가용성을 성공 조건으로 삼지 않으며 검색 결과나 모델 기억만으로 금융 규칙을 `verified`로 승격하지 않습니다.

### 지식 처리 규칙

- Markdown을 UTF-8로 읽고 hashing 전에 Unicode NFC와 LF로 정규화합니다.
- H1~H3 heading path를 보존해 section 단위로 분리합니다.
- paragraph 경계에서 약 1,500자 단위로 나누고 인접 text chunk는 200자를 겹칩니다.
- table과 fenced code block은 중간에서 나누지 않습니다. 기준 크기를 넘는 단일 block은 하나의 `oversized: true` chunk로 남기고 경고합니다.
- heading slug는 `Locale.ROOT`, Unicode letter·number 보존, 나머지 구분자 `-` 규칙으로 만듭니다.
- 같은 heading이 반복되면 document 안의 heading occurrence를 ID에 포함해 충돌을 막습니다.
- chunk ID는 `document-id + heading-slug + heading-occurrence + chunk-ordinal`로 결정론적으로 만듭니다.
- 각 chunk에 문서 ID, 제목 경로, source path와 정규화 전 원본 line 범위, status, domains, version, source/evidence reference, SHA-256 checksum을 포함합니다.
- 결과는 `build/knowledge/knowledge.jsonl`에 고정 key 순서와 마지막 newline을 사용해 기록하고 Git에서 제외합니다.

다음 상태는 `knowledgeCheck` 실패입니다.

- 중복 문서·chunk ID
- 존재하지 않거나 repository 밖을 가리키는 catalog path
- 존재하지 않는 source·evidence ID
- 깨진 내부 상대 링크나 heading anchor
- 허용하지 않는 kind·status
- source·evidence가 없는 `verified`
- 같은 입력에서 달라지는 chunk ID·본문·checksum

Linux와 Windows에서 같은 fixture를 처리해 동일 checksum을 확인합니다. golden fixture에는 CRLF/LF, 한글·중복 heading, 깨진 anchor, 긴 table, 긴 fenced block, 동일 이름 파일을 포함합니다.

Vector database와 embedding은 프로젝트 11에서 retrieval 요구와 평가 corpus가 확정된 뒤 도입합니다.

## 7. 프론트·계약 골격

- `frontend/README.md`와 `frontend/AGENTS.md`만 만들고 application이나 Node dependency는 생성하지 않습니다.
- framework 선택은 프로젝트 06의 관리자 UI 요구가 확정될 때 수행합니다.
- 미래 UI는 versioned OpenAPI만 소비하고 database나 backend repository에 접근하지 않습니다.
- client 분기는 사용자 메시지가 아니라 표준 오류 `code`를 사용합니다.
- `contracts/README.md`에 OpenAPI와 event schema의 소유권, versioning, backward compatibility, 생성물 처리 규칙을 기록합니다.
- 아직 존재하지 않는 endpoint나 schema를 미리 만들지 않습니다.
- 프로젝트 01의 HTTP API가 추가될 때 contract lint와 backend contract test를 `harnessFull`에 편입합니다.

## 8. 최소 CI 계약

`.github/workflows/harness.yml`은 저장소의 Gradle task만 호출하며 별도 검증 로직을 복제하지 않습니다.

- 기본 브랜치 `main` 대상 pull request에서 Linux·Windows `harnessFast`를 실행합니다.
- Linux에서는 `harnessFull`도 실행해 Docker와 PostgreSQL 통합 경로를 확인합니다.
- concurrency group으로 같은 PR의 이전 실행을 취소합니다.
- job timeout을 명시하고 무한 대기를 금지합니다.
- dependency cache key에는 wrapper, lockfile, verification metadata checksum을 포함합니다.
- action은 commit SHA로 고정하고 `contents: read` 외 권한은 기본 거부합니다.
- fork pull request에서도 secret 없이 실행돼야 합니다.
- 성공·실패 manifest와 test report를 `always()` 조건으로 artifact에 업로드하고 보존 기간을 명시합니다.
- branch protection의 required check 설정은 repository 관리자 작업으로 따로 기록합니다.

이 CI는 애플리케이션 배포 파이프라인이 아닙니다. AWS와 배포 credential은 프로젝트 06 범위에서 별도 ADR과 Terraform 계획으로 결정합니다.

## 9. 완료 조건

### 단계 A 완료

- `harnessDoctor`, `harnessFast`, `harnessFull`, `localUp`, `localDown`, `promoteHarnessEvidence`가 문서와 실제 task에서 일치합니다.
- wrapper checksum, dependency lock, dependency verification, repository allowlist가 실제로 적용됩니다.
- Linux·Windows Fast와 Linux Full이 최소 CI에서 성공합니다.
- Docker가 꺼진 환경에서 Fast는 성공하고 Full은 이해 가능한 preflight 오류로 실패합니다.
- cache 준비 후 `--offline harnessFast`가 성공합니다.
- 잘못된 모듈 dependency를 Gradle 경계와 architecture test가 거절합니다.
- Fresh migration, JPA validation, account 저장·조회, Boot context, executable artifact 생성이 성공합니다.
- Upgrade migration은 직전 상태를 검증하거나 첫 migration에서는 명시적인 NOT_APPLICABLE을 기록합니다.
- 같은 검증을 두 번 실행해도 `build/` 밖의 추적 파일이 변하지 않습니다.
- manifest의 SHA, 환경, task 결과, artifact checksum이 실제 결과와 일치합니다.
- evidence 승격이 SHA 불일치·실패 실행·checksum 충돌·secret 노출을 거절합니다.
- 프로젝트 01의 종료 기준 중 구현하지 않은 항목은 체크하거나 성과로 표현하지 않습니다.

### 단계 B 완료

- `knowledgeCheck`와 `knowledgeExport`가 문서와 실제 task에서 일치합니다.
- source와 evidence가 없는 `verified`, 깨진 내부 링크, 중복 ID, 잘못된 status를 거절합니다.
- 같은 입력을 두 번 처리한 JSONL의 chunk ID, 본문, metadata, checksum이 같습니다.
- Linux·Windows golden fixture 결과가 같습니다.
- heading path, 원본 source line, status, domain, source/evidence reference가 각 chunk에 유지됩니다.
- 긴 table과 code block이 잘리지 않고 `oversized` 상태가 기록됩니다.
- generated JSONL과 test report는 Git 추적 대상이 아닙니다.
- 단계 B 통합 뒤 `harnessFast`가 `knowledgeCheck`를 포함합니다.

### 2026-08-28 로컬 확인 기록

- `./gradlew harnessDoctor`, `./gradlew harnessFast`, `./gradlew harnessFull`: PASS
- `./gradlew --offline harnessFast`: PASS
- `:platform:bootstrap:integrationTest --rerun-tasks`: PostgreSQL 18.6 Fresh migration, JPA 저장·조회, Boot context PASS
- Gradle TestKit: 역방향 project dependency와 ledger persistence type 직접 접근 거절 PASS
- 지식 golden fixture: LF/CRLF 동일 출력, 중복 heading·동일 파일명 ID 분리, 긴 table·fenced block 보존, 깨진 anchor·잘못된 status 거절 PASS
- 같은 HEAD에서 `knowledgeExport`를 2회 실행해 SHA-256 동일성 확인
- 격리된 실패 주입: 잘못된 catalog는 FAILED manifest를 남기고, Docker 불가 Full은 `dockerPreflight`에서 복구 문구와 함께 실패
- `promoteHarnessEvidence` dirty worktree 거절 PASS. clean source SHA의 실제 evidence 승격은 기능 구현 commit 이후 별도 evidence commit에서 수행합니다.

위 기록은 구현 중 로컬 실행 결과이며 아직 승격된 프로젝트 evidence가 아닙니다. Linux·Windows CI 결과와 clean source SHA manifest는 원격 실행 뒤 확인합니다.

## 10. 알려진 한계와 후속 결정

- 최초 dependency·toolchain 다운로드에는 network가 필요할 수 있으며 완전한 air-gapped bootstrap은 이번 범위가 아닙니다.
- Linux Full만으로 Windows Docker 경로를 증명하지 않습니다. Windows에서는 Fast와 deterministic fixture까지만 필수로 검증합니다.
- Compose는 개발 편의, Testcontainers는 자동 검증이므로 두 환경의 성능 수치를 직접 비교하지 않습니다.
- GitHub branch protection, AWS, container registry, 배포 IAM, Terraform state는 이 계획이 아니라 저장소 운영 설정과 프로젝트 06 ADR에서 결정합니다.
- Testcontainers와 Compose image를 갱신할 때 tag뿐 아니라 digest, release note, 전후 Full 결과를 함께 기록합니다.
- 일부 upstream artifact는 서명이 없거나 공개키 서버에서 키를 내려받지 못합니다. 해당 목록은 `verification-metadata.xml`의 `ignored-key` reason으로 가시화하고, 이 경우에도 생성·검토한 SHA-256 일치를 필수로 유지합니다.

## 공식 기준 자료

- [Codex AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
- [Codex advanced configuration](https://learn.chatgpt.com/docs/config-file/config-advanced)
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Gradle 9.7.1 release notes](https://docs.gradle.org/9.7.1/release-notes.html)
- [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html)
- [Gradle wrapper validation](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:verification)
- [PostgreSQL 18.6 release notes](https://www.postgresql.org/docs/release/18.6/)
- [Testcontainers for Java](https://java.testcontainers.org/)
- [GitHub Actions secure use](https://docs.github.com/en/actions/reference/security/secure-use)
