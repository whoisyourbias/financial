# 전역 Git worktree 작업 규칙

변경 작업은 반드시 별도의 Git worktree에서 수행한다.

## 작업 시작 순서

1. 저장소의 기본 worktree 상태와 worktree 목록을 확인한다.
2. 기본 worktree에서 기본 브랜치로 전환한다.
   - 기본 브랜치가 `master`이면 `master`를 사용한다.
   - `master`가 없고 원격 저장소의 기본 브랜치가 `main`이면 `main`을 사용한다.
   - 그 밖의 기본 브랜치가 확인되면 해당 브랜치를 사용하되, 사용한 브랜치를 작업 보고에 명시한다.
3. 기본 브랜치에 미커밋 변경사항이 있으면 변경사항을 덮어쓰거나 삭제하지 말고 사용자에게 알린다.
4. 기본 worktree에서 원격 저장소의 최신 정보를 가져온다.
5. 기본 브랜치를 해당 원격 추적 브랜치(`origin/master`, `origin/main` 등) 기준으로 최신화한다.
6. 최신화된 기본 브랜치에서 작업용 브랜치와 별도 worktree를 생성한다.
   - 작업용 worktree는 반드시 기본 저장소 내부의 `.worktrees/<작업명>` 경로에 생성한다.
   - 저장소와 같은 상위 디렉터리에 sibling worktree를 만들거나 저장소 외부 경로를 사용하지 않는다.
   - `.worktrees/`가 Git 상태에 나타나지 않도록 기본 저장소의 `.git/info/exclude`에 등록한 뒤 생성한다.

기본 브랜치의 최신화와 작업용 worktree 생성이 완료되기 전에는 파일을 수정하지 않는다.

## 작업 중 및 완료 규칙

- 기본 worktree에서 직접 파일을 수정하지 않는다.
- 모든 작업용 worktree 경로는 `<기본 저장소>/.worktrees/<작업명>` 형식을 사용한다.
- 모든 수정, 테스트, 포맷팅, 커밋은 작업용 worktree에서 수행한다.
- 기존 작업용 worktree가 저장소 내부 `.worktrees/`에 있으면 새로 만들지 말고 해당 worktree를 사용한다.
- 기존 작업용 worktree가 저장소 외부에 있으면 변경사항 유실 여부를 확인한 뒤 `git worktree move`로 `.worktrees/` 아래로 이동한다.
- 단순 조회·분석 작업은 worktree 생성 없이 수행할 수 있다.
- 사용자가 명시적으로 예외를 요청하지 않는 한 이 규칙을 우회하지 않는다.
- 작업 완료 시 작업용 worktree 경로, 변경 파일, 테스트 결과를 보고한다.

# 금융 백엔드 프로젝트 작업 규칙

## 필수 문서 읽기 순서

1. `README.md`
2. `docs/TECH_BASELINE.md`
3. `docs/EVIDENCE_POLICY.md`
4. 현재 프로젝트의 `PLAN.md`와 `EXIT_CRITERIA.md`
5. 현재 작업 경로에서 가장 가까운 `AGENTS.md`

## 공통 불변식과 경계

- 금액 계산에 `double`과 `float`를 사용하지 않습니다.
- 구현하지 않았거나 재현하지 않은 결과를 `[검증됨]` 또는 성과로 표현하지 않습니다.
- 실제 고객 데이터, credential, 개인정보를 source·fixture·log·manifest에 넣지 않습니다.
- domain module은 다른 module의 JPA entity와 repository에 직접 접근하지 않습니다.
- migration은 Flyway 파일로만 수행하며 Hibernate schema generation이나 `repair`로 실패를 숨기지 않습니다.
- 일반 test는 `build/` 밖의 추적 파일을 수정하지 않습니다.

## 공통 검증 명령

- 빠른 검증: `./gradlew harnessFast`
- Docker 포함 전체 검증: `./gradlew harnessFull`
- 환경 진단: `./gradlew harnessDoctor`
- 지식 검증: `./gradlew knowledgeCheck`
- 지식 export: `./gradlew knowledgeExport`

Docker가 필요 없는 변경은 먼저 Fast로 검증합니다. Full을 실행할 수 없는 환경에서는 실패 원인과 실행하지 못한 범위를 결과에 명시합니다.
