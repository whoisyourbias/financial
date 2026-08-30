# 프로젝트 종료 평가 게이트

## 상태 모델

```text
PLANNED → IN_PROGRESS → EVIDENCE_READY → REVIEWED → RELEASE_CANDIDATE → RELEASED
                  └───────────────────────────────→ HOLD ←──────────────────┘
```

| 상태           | 진입 조건                                                           |
| -------------- | ------------------------------------------------------------------- |
| PLANNED        | 상세 계획과 종료 기준이 존재                                        |
| IN_PROGRESS    | 범위와 기준선 commit이 기록됨                                       |
| EVIDENCE_READY | 필수 테스트·측정·문서와 검증된 manifest가 존재                       |
| REVIEWED | 동결 payload `C0`를 가리키는 두 tracked review attestation이 직계 후속 `C1`에 존재 |
| RELEASE_CANDIDATE | 두 attestation이 같은 `reviewedSha`에 PASS·High 0이고 clean manifest와 intended tags 준비 |
| RELEASED | annotated Git tag가 candidate commit을 가리키고 tag 검증이 통과 |
| HOLD | 기능 불변식 실패, 필수 검증 미실행, 근거 부족, 정합성 🔴 또는 해결되지 않은 핵심 위험 |

`RELEASED`는 실서비스 출시를 뜻하지 않습니다. 해당 2주 프로젝트의 증거와 문서가 공개 가능한 상태라는 뜻입니다.

## 게이트 1 — 구현·증거 준비

- 필수 범위가 실행 가능하고 확장 범위와 구분되어 있습니다.
- 자동화 테스트 명령이 재현됩니다.
- 실패 시나리오가 정상 경로와 함께 검증됩니다.
- `projects/<id>/evidence/MANIFEST.md`에서 결과가 `sourceSha`와 환경으로 연결되고 `./gradlew evidenceCheck -PtargetProject=<id>`가 통과합니다.
- 결과표가 raw log·query·dashboard로 역추적됩니다.
- 계획 다이어그램이 아니라 현재 구현 다이어그램이 존재합니다.
- known limitation과 미해결 문제가 공개되어 있습니다.

하나라도 없으면 `EVIDENCE_READY`로 이동하지 않습니다.

## 게이트 2 — Hiring Portfolio Review

`hiring-sim-portfolio-review`를 프로젝트 디렉터리 전체에 적용합니다.

### 영역 1: 기술적 의사결정

`적합`을 위한 최소 조건:

- 해결하려는 메커니즘이 증상과 구분되어 있습니다.
- 실제 검토하거나 실험한 대안만 기록합니다.
- 선택 근거와 감수한 비용을 함께 설명합니다.
- 사용한 기술과 현재 구조가 코드·다이어그램에 일치합니다.

`우수` 후보 조건:

- 대안을 같은 조건에서 비교한 evidence가 있습니다.
- 선택으로 생긴 새 실패 모드와 완화책을 설명합니다.
- 남은 한계를 다음 회차의 문제로 연결합니다.

### 영역 2: 문제 해결 과정

`적합`을 위한 최소 조건:

- 금융 불변식과 제약이 테스트 가능한 문장입니다.
- 정상·경계·실패 시나리오가 자동화 테스트와 연결됩니다.
- 실패한 접근이나 수정 이유를 삭제하지 않고 기록합니다.

### 영역 3: 성과·임팩트

`적합`을 위한 최소 조건:

- 최소 한 개의 정량 결과가 조건·원본과 함께 있습니다.
- 개인 프로젝트라는 기여 범위가 명확합니다.
- 실제 배포가 없으면 배포했다고 표현하지 않습니다.

`우수`는 정량 근거, 맥락, 본인 기여, 배포·사용 여부가 모두 확인될 때만 가능합니다. 로컬 실험만으로 억지로 우수를 만들지 않습니다.

### PASS

- 세 영역 모두 `적합` 이상
- 대표 주장에 🔴 정합성 결함 없음
- 포지션 정렬도 충분

하나라도 충족하지 못하면 REJECT이며 구체적인 보완 후 다시 평가합니다.

## 게이트 3 — Adversarial Red Team

Portfolio Review 후 `hiring-sim-portfolio-redteam`을 같은 디렉터리에 적용합니다.

### L1: 모든 수치

- 단위와 백분위가 같은가?
- 평균·피크·합계 관계가 산술적으로 가능한가?
- 분모와 제외 조건이 같은가?
- 표·dashboard·본문 값이 같은가?

### L2: 서사와 근거

- 설명한 아키텍처가 실제 코드·설정·배포와 같은가?
- 비동기 접수와 업무 완료를 바꿔 부르지 않았는가?
- 사용하지 않은 기술을 스택에 넣지 않았는가?
- 공개 문서에 빈 결과·미완성 캡처가 남지 않았는가?

### L3: 최악의 면접관

핵심 주장마다 다음 질문에 답합니다.

1. 이 수치가 실제 사용자 환경과 무슨 관계가 있는가?
2. 다른 변경이 결과에 영향을 주지 않았다는 근거가 있는가?
3. 왜 더 단순한 방법을 쓰지 않았는가?
4. 이 선택으로 새로 생긴 실패 모드는 무엇인가?
5. 재현되지 않으면 무엇을 주장 철회할 것인가?

### 기술적 고민 강화

각 핵심 결정은 다음 다섯 요소를 확인합니다.

1. 문제의 메커니즘
2. 실제 대안 비교
3. 선택 근거
4. 감수한 비용
5. 검증과 남은 한계

없는 요소를 꾸며내지 않고 `미검토` 또는 `한계`로 남깁니다.

Docker가 필요한 필수 범위는 `./gradlew harnessFull`이 통과해야 합니다. 실행할 수 없는 이유는 반드시 기록하지만 면제 조건이 아니며, 그 프로젝트는 실행 가능한 환경에서 재검증할 때까지 `HOLD`입니다.

## RELEASE_CANDIDATE/RELEASED/HOLD 판정

### RELEASE_CANDIDATE

- Portfolio Review PASS
- 🔴 High 0건
- 🟠 Medium은 수정됐거나 공개 limitation으로 승인됨
- 필수 면접 질문에 evidence 링크로 답변 가능
- clean manifest의 `sourceSha`와 `intendedTags` 준비 완료
- `sourceSha..candidate` diff가 증거·결과·리뷰·패키징 문서 allowlist만 포함
- `C0..C1`은 두 review report만 포함하고 두 report의 `reviewedSha`가 `C0`로 일치

### RELEASED

- `RELEASE_CANDIDATE`의 동일 commit에 annotated tag 생성
- 각 tag tree에 clean manifest가 있고 `sourceSha`가 tag commit의 조상이며 이름이 `intendedTags`에 포함
- 중간 diff에 코드·설정·migration·contract·OpenAPI 변경이 있으면 기존 evidence를 폐기하고 새 source에서 재실행
- tag는 두 report가 같은 payload를 증명하는 attestation commit `C1`을 가리킴
- tag 검증 실패 또는 tag 이후 내용 변경 시 다시 `RELEASE_CANDIDATE`부터 평가

### HOLD

- 금융 불변식 실패
- 대표 수치 원본 부재 또는 모순
- 측정 대상 바꿔치기
- 보안·권한 우회
- 합성 실험을 실서비스 성과로 표현
- AI가 승인 없이 변경을 실행
- 핵심 용어를 잘못 사용해 설명 전체가 흔들림

HOLD 사유는 삭제하지 않고 프로젝트 결과에 남깁니다. 수정 뒤 두 평가를 모두 다시 실행합니다.

## 프로젝트별 종료 문서

구현이 시작되면 각 프로젝트 폴더에 다음 결과를 추가합니다.

```text
RESULTS.md
PORTFOLIO_REVIEW.md
REDTEAM_REVIEW.md
evidence/
```

- `RESULTS.md`: 구현, 측정, 한계, 다음 회차 영향
- `PORTFOLIO_REVIEW.md`: PASS/REJECT와 세 영역 근거
- `REDTEAM_REVIEW.md`: 심각도, 모순 대조, 면접 질문, 개선
- `evidence/`: 원본과 manifest

계획 단계의 빈 결과 파일은 만들지 않습니다. 빈 문서가 완성본처럼 보이는 문제를 예방하기 위해 실제 evidence가 생긴 뒤 생성합니다.

## 전체 감사

최종 2주에는 개별 PASS를 그대로 신뢰하지 않고 다시 전수 대조합니다.

- 프로젝트 사이에서 같은 지표가 다른 뜻으로 사용되지 않았는지 확인
- 이전 다이어그램과 최종 구조를 구분
- 대표 README의 숫자를 원본까지 역추적
- 네 대표 사례 외의 반복 기술 나열 제거
- 지원 시점의 실제 공고와 다시 매핑
- 임시 AWS 리소스 제거와 비용 확인

세부 일정은 [FINAL_AUDIT_PLAN.md](../reviews/FINAL_AUDIT_PLAN.md)를 따릅니다.
