# 프로젝트 12 — 책임 있는 결제 운영 에이전트

## 목표

결제 운영자가 조회·진단·조치 초안을 빠르게 만들되, 금액이나 외부 상태를 바꾸는 도구는 정책·승인·멱등성·감사 경계 안에서만 실행하게 합니다.

## 선행 조건

- 프로젝트 02의 승인·취소 멱등성
- 프로젝트 03의 웹훅 전달·재전송
- 프로젝트 04의 가상계좌 대사
- 프로젝트 05의 MID·역할·감사
- 프로젝트 08의 정산 adjustment
- 프로젝트 10의 위험 신호
- 프로젝트 11의 근거 있는 검색

## 권한·위험·credential 경계

`LOW_RISK`와 `FINANCIAL`은 action의 risk class이며 Agent 권한 이름이 아닙니다. Agent는 항상 `READ`와 `PROPOSE`만 가집니다.

| risk class | Agent 동작 | 실제 호출 주체 | credential 소유자 | 승인자 | executor |
| --- | --- | --- | --- | --- | --- |
| `READ_ONLY` | 허용된 schema로 조회·근거 인용 | Agent read adapter | 범위 제한 read service account | 불필요 | read service |
| `LOW_RISK` | 웹훅 재전송 같은 조치 초안만 생성 | 인증된 사람의 별도 approval API | deterministic operations service | 해당 MID 역할의 사람 | 승인 ID를 검증한 operations service |
| `FINANCIAL` | 취소·정산 조정 초안만 생성 | 인증된 사람의 별도 approval API | deterministic payment/settlement service | 금액 권한이 있는 사람 | 승인 ID·버전·만료·요청 해시를 재검증한 payment/settlement service |

Agent process에는 approval·execute credential을 주입하지 않습니다. 승인자는 Agent 세션이 아닌 별도 인증 API에서 승인하고, executor는 모델 출력이 아니라 versioned proposal과 approval record만 입력으로 받습니다.

## 필수 범위

- read-only 타임아웃 진단 1개와 근거 인용
- 부분 취소 proposal 1개를 사람 승인 API와 deterministic executor까지 연결
- stale approval·replay·cross-MID·prompt injection 공격
- 신규·queued·pre-commit 실행을 막는 kill switch와 수동 대체 런북

웹훅 재전송, 가상계좌 대사, 정산 adjustment 시나리오는 선행 프로젝트 데이터를 조회하는 contract test까지만 필수입니다. 추가 쓰기 도구와 운영 UI는 확장 범위이며 필수 게이트 완료 전 시작하지 않습니다.

## 불변식

1. 검색 결과나 모델 출력은 도구 권한이 아닙니다.
2. 모든 도구 호출은 인증된 MID와 역할 범위 안에서 실행합니다.
3. 승인에는 요청 해시, 대상, 금액, 만료 시각을 묶습니다.
4. 승인 뒤 상태가 바뀌면 실행하지 않고 새 계획을 요구합니다.
5. 쓰기 도구는 멱등키와 전후 상태를 감사 로그에 남깁니다.
6. prompt injection은 정책·도구 스키마·MID 경계를 변경하지 못합니다.
7. 모델 장애 시 사람이 같은 런북으로 작업할 수 있습니다.
8. 금융 write의 linearization point는 executor의 도메인 transaction commit입니다. kill switch는 신규·queued 요청을 거절하고 pre-commit에서 다시 확인하지만, commit 이후 효과를 rollback했다고 표현하지 않습니다.
9. commit 이후 실패나 외부 효과는 감사·조회·대사와 명시적 보상 command로 처리하며 model/policy rollback과 금융 보상을 구분합니다.

## 대표 시나리오

- 승인 타임아웃 진단과 조회 순서 제안
- 실패 웹훅의 원인 요약과 수동 재전송 제안
- 가상계좌 대사 차이의 근거 수집
- 부분 취소 초안, FDS 검토, 사람 승인, 실행
- 정산 adjustment 설명과 티켓 초안

## 구현 순서

1. 도구 카탈로그와 risk class를 고정합니다.
2. policy engine을 모델과 분리해 결정론적으로 구현합니다.
3. read-only 진단과 근거 인용을 연결합니다.
4. propose → approve → revalidate → execute 상태 머신을 구현합니다.
5. prompt injection, 권한 상승, replay를 공격 테스트합니다.
6. kill switch, model/policy rollback, commit 이후 대사·보상과 수동 대체 런북을 구분해 검증합니다.

## 평가

- 정책 위반 실행 0건
- 승인 없는 금융 쓰기 0건
- stale approval 차단율
- 중복 실행 방지율
- 근거 없는 제안 보류율
- 사람 검토 시간은 원시 측정 범위 안에서만 보고

## 검증 산출물

- 도구별 권한·위험 표
- 정책 결정 golden test
- red-team prompt와 결과
- 승인·재검증·멱등 실행 통합 테스트
- kill switch 훈련 기록
- `projects/12-responsible-ops-agent/evidence/` manifest

## 근거

- 결제 계약과 운영 근거는 `docs/TOSS_PAYMENTS_CONTRACT_MATRIX.md`, `docs/TOSS_PAYMENTS_EVIDENCE_MAP.md`를 사용합니다.
- 에이전트 정책은 저장소 고유 설계이며 토스페이먼츠 내부 자동화를 재현한다고 주장하지 않습니다.
