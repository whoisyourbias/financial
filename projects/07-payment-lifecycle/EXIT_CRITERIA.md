# 프로젝트 07 종료 기준

## 기능·정합성 게이트

- [ ] 상태 전이표의 모든 허용 전이가 구현됩니다.
- [ ] 표에 없는 전이는 금전 변경 없이 거절됩니다.
- [ ] capture·refund 한도가 동시 요청에서도 지켜집니다.
- [ ] capture와 cancel이 동시에 성공하지 않습니다.
- [ ] 같은 멱등 키는 한 operation 결과만 만듭니다.
- [ ] payment 순매입과 원장 capture·refund 순액이 같습니다.
- [ ] 상태·history·journal의 부분 commit이 없습니다.

## 테스트·증거 게이트

- [ ] 전이표 각 cell과 자동화 test가 연결됩니다.
- [ ] 동시 cancel/capture와 refund 결과 원본이 있습니다.
- [ ] payment·operation·journal query가 같은 correlation로 대조됩니다.
- [ ] partner·ledger 실패의 rollback 결과가 있습니다.
- [ ] idempotency key당 고유 operation 수가 확인됩니다.
- [ ] 실제 PG 미연동과 미지원 결제 범위가 명시돼 있습니다.
- [ ] diagram과 현재 단일 process 구조가 일치합니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                           |
| --------------- | --------------------------------------------------- |
| 기술적 의사결정 | 상태 전이 표현과 동기 ledger 경계 선택·분리 시 비용 |
| 문제 해결       | 동시 전이, 부분 환불, 외부·ledger 실패 처리         |
| 성과·임팩트     | 전이표 coverage와 payment·journal 순액 원본         |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] 상태 전이표·코드·문서가 일치합니다.
- [ ] 대표 결제 주장에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] stub을 실제 PG·카드사 연동으로 표현하지 않았습니다.
- [ ] authorize·capture·settlement를 같은 단계로 표현하지 않았습니다.
- [ ] operation history를 event sourcing으로 표현하지 않았습니다.
- [ ] 합성 동시 요청을 상용 결제 처리량으로 표현하지 않았습니다.
- [ ] 환불 API 응답시간과 원장 반영 완료시간을 구분했습니다.
- [ ] 결제 성공률을 비즈니스 성과로 과장하지 않았습니다.

## 면접 방어

- [ ] 모든 상태 전이와 금전 기록을 설명할 수 있습니다.
- [ ] cancel/capture 경쟁의 transaction·lock 방식을 설명할 수 있습니다.
- [ ] 부분 환불 누적 한도 방어를 설명할 수 있습니다.
- [ ] 서비스 분리 시 깨지는 원자성 가정을 설명할 수 있습니다.
- [ ] 실제 결제 시스템과 다른 비범위를 말할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록        |
| ---------------- | ---------------------------- |
| Portfolio Review | PASS 또는 REJECT             |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수 |
| 상태·원장 정합성 | 원본 query 경로              |
| Evidence         | commit SHA와 manifest 경로   |
| 최종 상태        | RELEASED 또는 HOLD           |

불법 전이가 성공하거나 capture·refund 순액이 원장과 다르거나 🔴가 남으면 태그를 생성하지 않습니다.
