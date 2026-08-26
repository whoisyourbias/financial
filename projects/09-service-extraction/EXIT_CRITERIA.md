# 프로젝트 09 종료 기준

## 실험·정합성 게이트

- [ ] 분리 전 modular baseline이 같은 commit·조건으로 보존됩니다.
- [ ] Payment Service가 payment data를 소유하고 ledger table에 접근하지 않습니다.
- [ ] core가 payment table에 직접 접근하지 않습니다.
- [ ] timeout·duplicate·reorder에 API·consumer가 멱등 처리됩니다.
- [ ] payment·ledger 불일치가 숨겨지지 않고 pending·difference로 노출됩니다.
- [ ] old/new contract 조합이 테스트됩니다.
- [ ] module 상태로의 rollback rehearsal이 성공합니다.

## 비교·증거 게이트

- [ ] 분리 전후 use case·fault schedule·dataset·request mix가 같습니다.
- [ ] topology 차이는 별도 명시돼 있습니다.
- [ ] latency·pending·failure point·runbook·cost를 함께 비교했습니다.
- [ ] architecture diagram과 실제 process·DB가 일치합니다.
- [ ] network·ledger failure의 timeline과 원본 상태가 있습니다.
- [ ] rollback 명령과 회귀 테스트 결과가 있습니다.
- [ ] 최종 결론이 근거와 함께 유지·분리·원복 중 하나로 기록됩니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                            |
| --------------- | ---------------------------------------------------- |
| 기술적 의사결정 | 모듈 유지와 분리의 동일 조건 비교, 새 실패·운영 비용 |
| 문제 해결       | 네트워크·두 DB·contract·rollback 문제의 명시적 처리  |
| 성과·임팩트     | before/after 원본과 결론·한계의 정합                 |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] 결론이 계획과 달라도 evidence를 우선했습니다.
- [ ] 대표 아키텍처 주장에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] 서비스 하나를 분리한 것을 MSA 운영 경험으로 표현하지 않았습니다.
- [ ] broker 도입을 SPOF 제거로 표현하지 않았습니다.
- [ ] 분리 전후 다른 topology 숫자를 직접 비교하지 않았습니다.
- [ ] 코드 줄 수 하나로 결합도·생산성 개선을 주장하지 않았습니다.
- [ ] payment API 응답과 ledger 최종 반영시간을 구분했습니다.
- [ ] 계획에 있던 분리를 사후에 필연적 선택처럼 쓰지 않았습니다.

## 면접 방어

- [ ] 분리 전후 가장 큰 이점과 비용을 각각 설명할 수 있습니다.
- [ ] 두 DB 불일치 상태와 복구 흐름을 설명할 수 있습니다.
- [ ] contract compatibility와 consumer rollout을 설명할 수 있습니다.
- [ ] rollback 시 데이터·이벤트 처리를 설명할 수 있습니다.
- [ ] 현재 규모에서 분리가 과도할 수 있는 이유를 말할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록        |
| ---------------- | ---------------------------- |
| Portfolio Review | PASS 또는 REJECT             |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수 |
| 최종 결론        | 유지·분리·원복과 근거        |
| Evidence         | commit SHA와 manifest 경로   |
| 최종 상태        | RELEASED 또는 HOLD           |

동일 조건 비교가 없거나 데이터 소유권을 공유 DB로 우회하거나 rollback이 실패하거나 🔴가 남으면 태그를 생성하지 않습니다.
