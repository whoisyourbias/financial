# 하네스 엔지니어링 구현 계획

## 상태와 목적

| 항목 | 값 |
| --- | --- |
| 상태 | APPROVED |
| 기준일 | 2026-08-27 |
| 적용 범위 | 저장소 공통 하네스와 프로젝트 01 실행 골격 |
| 기본 원칙 | 에이전트 중립형, 로컬 우선, Fast/Full 2단계 검증 |

이 계획은 에이전트가 금융 백엔드 코드를 일관되게 이해·수정·검증하도록 저장소가 소유하는 실행 규약을 정의합니다. Codex 전용 설정은 공통 명령과 지식 문서를 연결하는 얇은 어댑터로 제한합니다.

이번 단계에는 프로젝트 01의 빌드·모듈·데이터베이스 smoke test까지 포함합니다. 프론트엔드는 프로젝트 06에서 구현하며, 지금은 API 계약과 품질 규칙만 예약합니다. GitHub Actions, vector database, embedding, 실제 원장 API는 이번 범위에서 제외합니다.

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
- lifecycle hook은 첫 단계에 추가하지 않습니다. 결정론적 검사는 Gradle task로 제공해 다른 에이전트와 사람도 같은 경로를 사용하게 합니다.
- 새 Codex session에서 활성화된 루트·하위 지침과 명령을 요약하게 해 instruction discovery를 확인합니다.

### 공개 명령

모든 명령은 Windows에서 `gradlew.bat`, 다른 환경에서 `./gradlew`로 실행합니다.

| 명령 | 역할 | Docker 필요 |
| --- | --- | --- |
| `harnessDoctor` | Java, Docker, repository 설정과 필수 파일 확인 | 아니요 |
| `harnessFast` | 지식 검증, compile, unit test, architecture test | 아니요 |
| `harnessFull` | Fast, PostgreSQL·Flyway·JPA integration test, packaging | 예 |
| `knowledgeCheck` | metadata, 링크, 출처, 중복 ID 검증 | 아니요 |
| `knowledgeExport` | 검색·RAG 입력용 JSONL 생성 | 아니요 |

Docker daemon이 정지된 경우 `harnessFull`은 원인과 필요한 조치를 명시한 preflight 오류로 종료합니다. 일반 test 실행은 추적 파일을 수정하지 않습니다.

## 2. 도메인 지식과 검색·RAG 준비

### 지식 catalog

기존 문서 내용을 복제하지 않고 `docs/domain/catalog.yaml`에서 검색 대상과 metadata를 관리합니다.

필수 필드는 다음과 같습니다.

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
| `sourceRefs` | `sources.yaml`의 출처 ID 목록 |

`status`는 기존 `[가정]`, `[검증됨]`, `[불명]`, `[모순]` 정책과 각각 대응합니다. `verified`는 재현 가능한 evidence 또는 등록된 공식 출처 없이 사용할 수 없습니다.

### 외부 출처

`docs/domain/sources.yaml`에는 다음 정보만 저장합니다.

- 고유 출처 ID
- 공식 문서 제목과 URL
- 발행 기관
- 마지막 확인일
- 적용 도메인과 사용 범위
- 원문을 저장하지 않는다는 표시

변경 가능성이 있는 외부 자료는 사용할 때 원문을 다시 확인합니다. 검색 결과나 모델 기억만으로 금융 규칙을 `verified`로 승격하지 않습니다.

### 지식 처리 도구

`tools/knowledge-harness`를 Java application module로 만들고 다음 규칙을 구현합니다.

- Markdown의 H1~H3 heading path를 보존해 section 단위로 분리합니다.
- 긴 section은 paragraph 경계에서 약 1,500자 단위로 나누고 200자를 겹칩니다.
- table과 fenced code block은 중간에서 나누지 않습니다.
- chunk ID는 `document-id + heading-slug + ordinal`로 결정론적으로 만듭니다.
- 각 chunk에 문서 ID, 제목 경로, source path와 line, status, domains, version, SHA-256 checksum을 포함합니다.
- 결과는 `build/knowledge/knowledge.jsonl`에 기록하고 Git에서 제외합니다.

다음 상태는 `knowledgeCheck` 실패입니다.

- 중복 문서·chunk ID
- 존재하지 않는 catalog path 또는 source ID
- 깨진 내부 링크
- 허용하지 않는 kind·status
- 출처나 evidence가 없는 `verified`
- 같은 입력에서 달라지는 chunk ID·checksum

Vector database와 embedding은 프로젝트 11에서 retrieval 요구와 평가 corpus가 확정된 뒤 도입합니다.

## 3. 프로젝트 01 백엔드 골격

### 버전과 빌드

| 영역 | 고정 버전·방식 |
| --- | --- |
| JVM | Java 21 toolchain |
| Build | Gradle Wrapper 9.7.1, Kotlin DSL |
| Application | Spring Boot 4.1.1 |
| Database | PostgreSQL 18.6 container |
| Persistence | Spring Data JPA |
| Schema | Flyway only |
| Test | JUnit 5, Testcontainers, ArchUnit |

Spring Boot BOM이 관리하는 library version은 개별 선언하지 않습니다. dependency lock과 wrapper checksum을 추적하고 버전 변경은 기능 변경과 분리합니다.

### 모듈 구조

```text
platform/
├── bootstrap
├── ledger
└── shared-kernel
tools/
└── knowledge-harness
```

- package root는 `dev.whoisyourbias.financial`을 사용합니다.
- 의존 방향은 `bootstrap -> ledger -> shared-kernel`만 허용합니다.
- `shared-kernel`에는 `Money`, identifier, time abstraction, 공통 오류 계약만 허용합니다.
- `ledger`의 JPA entity와 repository는 다른 모듈에 공개하지 않습니다.
- `bootstrap`은 조립과 application 시작만 담당하며 domain rule을 소유하지 않습니다.
- ArchUnit으로 역방향 의존과 다른 모듈의 entity·repository 직접 접근을 실패시킵니다.

### 첫 데이터베이스 smoke slice

- `shared-kernel`에 `Money(amountMinor, currency)`를 추가하고 currency 불일치, 음수, overflow 경계를 단위 테스트합니다.
- 첫 Flyway migration은 `ledger_account` table만 생성합니다.
- account에는 UUID, account type, ISO-4217 currency, 생성 시각을 저장하며 balance column은 두지 않습니다.
- JPA는 `ddl-auto=validate`와 Open EntityManager in View 비활성화를 기본값으로 사용합니다.
- Testcontainers PostgreSQL에서 migration 실행, JPA mapping 검증, account 저장·조회, Boot context 시작을 확인합니다.
- journal, posting, HTTP API, 인증, outbox는 이 작업에서 구현하지 않습니다.

## 4. 프론트·계약·증거 골격

### 프론트엔드

- `frontend/README.md`와 `frontend/AGENTS.md`만 만들고 application이나 Node dependency는 생성하지 않습니다.
- framework 선택은 프로젝트 06의 관리자 UI 요구가 확정될 때 수행합니다.
- 미래 UI는 versioned OpenAPI만 소비하고 database나 backend repository에 접근하지 않습니다.
- client 분기는 사용자 메시지가 아니라 표준 오류 `code`를 사용합니다.
- 역할별 접근, keyboard navigation, 주요 accessibility 검사와 Playwright scenario를 종료 기준에 포함합니다.

### API·이벤트 계약

- `contracts/README.md`에 OpenAPI와 event schema의 소유권, versioning, backward compatibility, 생성물 처리 규칙을 기록합니다.
- 아직 존재하지 않는 endpoint나 schema를 미리 만들지 않습니다.
- 프로젝트 01의 HTTP API가 추가될 때 contract lint와 backend contract test를 `harnessFull`에 편입합니다.

### 증거

- 검증 결과는 `build/reports/harness/manifest.json`에 생성합니다.
- manifest에는 명령, Git SHA, 실행 시각, Java·Gradle·PostgreSQL 버전, test result, knowledge catalog checksum을 포함합니다.
- 프로젝트 evidence로 승격하는 작업은 별도 명시적 명령으로 두어 일반 검사가 `projects/*/evidence`를 수정하지 않게 합니다.

## 5. 검증 시나리오와 완료 조건

### Fast 검증

- 전역 Gradle과 Docker 없이 wrapper로 실행됩니다.
- 전체 module compile과 unit test가 성공합니다.
- 잘못된 module dependency를 ArchUnit이 거절합니다.
- 깨진 지식 링크, 중복 ID, 잘못된 status, 출처 없는 `verified`를 거절합니다.
- build 외의 추적 파일을 생성·수정하지 않습니다.

### Full 검증

- Docker daemon preflight를 통과합니다.
- PostgreSQL 18.6 container가 시작되고 빈 database에 Flyway migration이 적용됩니다.
- Hibernate schema validation과 account 저장·조회가 성공합니다.
- Spring Boot context와 executable artifact 생성이 성공합니다.
- Fast 검증을 포함하고 machine-readable manifest를 생성합니다.

### 지식 export 검증

- 같은 commit에서 두 번 실행한 결과의 chunk ID, 본문, checksum이 같습니다.
- heading path, source line, status, domain metadata가 각 chunk에 유지됩니다.
- table과 code block이 중간에서 잘리지 않습니다.
- generated JSONL과 test report는 Git 추적 대상이 아닙니다.

### 하네스 완료 조건

- `harnessDoctor`, `harnessFast`, `harnessFull`, `knowledgeCheck`, `knowledgeExport`가 문서와 실제 Gradle task에서 일치합니다.
- Codex 새 session에서 root와 현재 작업 subtree의 지침을 올바른 순서로 인식합니다.
- Docker가 꺼진 환경에서도 Fast 검증은 성공하고 Full 검증은 이해 가능한 오류를 반환합니다.
- 프로젝트 01의 종료 기준 중 구현하지 않은 항목은 체크하거나 성과로 표현하지 않습니다.
- GitHub Actions는 후속 작업에서 동일한 Fast/Full 명령만 호출하도록 추가합니다.

## 공식 기준 자료

- [Codex AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
- [Codex advanced configuration](https://learn.chatgpt.com/docs/config-file/config-advanced)
- [Spring Boot](https://spring.io/projects/spring-boot/)
- [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
- [PostgreSQL 18.6 release notes](https://www.postgresql.org/docs/release/18.6/)
