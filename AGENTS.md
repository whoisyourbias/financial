# Git Worktree 작업 규칙

이 저장소의 모든 변경 작업은 독립된 Git worktree에서 수행합니다. `main`은 릴리스 브랜치, `develop`은 통합 브랜치이며, 일반 작업은 `origin/develop`에서 분기한 `codex/<task-name>` 브랜치를 사용합니다.

하위 디렉터리의 `AGENTS.md`는 이 규칙을 구체화할 수 있지만, worktree 격리, 변경사항 보호, 브랜치 이력 보호 규칙을 완화할 수 없습니다.

## 브랜치 역할

- `main`: 릴리스 전용 브랜치입니다. 일반 개발의 시작점이나 직접 push 대상이 아닙니다.
- `develop`: 통합 브랜치입니다. 작업 브랜치의 기준이며 PR 대상입니다.
- `codex/<task-name>`: 하나의 작업만 포함하는 작업 브랜치입니다. `<task-name>`은 소문자 영문, 숫자, 하이픈을 사용합니다.
- 저장소 기본 worktree는 `main`을 유지합니다. `main`과 `develop` worktree에서는 구현 파일을 직접 수정하거나 커밋하지 않습니다.

## 작업 시작 절차

다음 순서를 모두 완료하기 전에는 추적 파일을 수정하지 않습니다.

1. 저장소 루트에서 `git status --short --branch`와 `git worktree list`로 기본 worktree, 기존 작업 worktree, 미커밋 변경을 확인합니다.
2. 각 기존 worktree의 상태도 확인합니다. 미커밋 변경은 사용자의 작업으로 간주하며 덮어쓰거나 삭제하지 않습니다.
3. `git fetch --prune origin`으로 원격 참조를 갱신합니다.
4. 기본 worktree의 `main`을 `origin/main` 기준으로 `--ff-only` 최신화합니다.
5. 기존 `develop` worktree를 재사용해 `origin/develop` 기준으로 `--ff-only` 최신화합니다. 없으면 저장소 내부 `.worktrees/develop`에 생성합니다.
6. `.git/info/exclude`에 `.worktrees/`가 등록되어 있는지 확인합니다.
7. 동일 작업의 내부 worktree가 있으면 새로 만들지 말고 상태를 확인한 뒤 재사용합니다.
8. 새 작업이면 최신 `origin/develop`에서 `codex/<task-name>` 브랜치를 만들고 `<repository>/.worktrees/<task-name>`에 worktree를 생성합니다.

예시:

```bash
git fetch --prune origin
git worktree add -b codex/example-task .worktrees/example-task origin/develop
```

원격 저장소나 추적 브랜치가 없으면 임의의 기준 브랜치를 만들지 않고 현재 상태를 보고합니다.

## 작업 중 규칙

- 파일 수정, 생성, 삭제, 포맷팅, 테스트, 코드 생성, 커밋은 작업 worktree에서만 수행합니다.
- 작업 범위와 무관한 기존 변경은 수정하거나 커밋하지 않습니다.
- 다른 작업의 worktree나 브랜치를 재사용하지 않습니다.
- 저장소 외부에 worktree를 만들지 않습니다. 기존 외부 worktree는 변경 유실 여부와 대상 경로를 확인하기 전에는 이동하지 않습니다.
- 생성물과 캐시는 `.gitignore` 정책을 따르며, 검증 명령이 의도하지 않은 추적 파일을 변경하지 않았는지 확인합니다.
- 실제 검토하지 않은 대안, 실행하지 않은 테스트, 측정하지 않은 결과를 문서나 커밋에 기록하지 않습니다.

## 충돌과 예외 처리

다음 상황에서는 작업을 중단하고 브랜치, worktree, 변경 파일, divergence 상태를 보고합니다.

- `main`, `develop` 또는 대상 작업 worktree에 미커밋 변경이 있음
- 로컬 브랜치와 원격 추적 브랜치를 fast-forward할 수 없음
- 예상하지 않은 merge conflict 또는 다른 작업과 겹치는 변경이 발견됨
- 작업 브랜치나 worktree 경로가 이미 다른 목적으로 사용 중임
- 원격의 기준 브랜치가 삭제됐거나 접근할 수 없음

사용자 승인 없이 `git reset --hard`, 강제 push, rebase, 이력 재작성, 추적 파일 삭제를 수행하지 않습니다. 충돌을 숨기기 위한 merge commit도 임의로 만들지 않습니다.

## 검증과 완료 절차

1. 작업 worktree에서 변경 범위에 맞는 테스트와 정적 검사를 실행합니다.
2. 문서 변경은 Markdown lint와 `git diff --check`를 실행합니다.
3. `git status --short`와 `git diff --stat`로 의도한 파일만 변경됐는지 확인합니다.
4. 검증 결과가 성공한 뒤 작업 브랜치에 커밋합니다.
5. `codex/<task-name>`을 원격에 push하고 upstream을 설정합니다.
6. 병합 대상은 `develop`로 유지합니다. `develop` 또는 `main`에 직접 push하거나 자동 병합하지 않습니다.
7. `main` 반영은 별도의 릴리스 작업에서 명시적인 승인과 검증 후 수행합니다.

완료 보고에는 다음을 포함합니다.

- 작업 worktree 절대 경로
- 기준 브랜치, 작업 브랜치, commit SHA
- 변경 파일과 핵심 변경 내용
- 실행한 검증과 결과
- 원격 push 또는 PR 상태
- 남은 제한사항이나 후속 작업

## 정리 규칙

- 작업 완료만으로 worktree, 로컬 브랜치, 원격 브랜치를 자동 삭제하지 않습니다.
- worktree 제거 전 변경사항, commit, 원격 push 여부를 다시 확인합니다.
- worktree 삭제, 브랜치 삭제, prune은 사용자 요청 또는 명시적인 정리 작업에서만 수행합니다.
