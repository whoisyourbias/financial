# 프로젝트 03 종료 기준

## 기능·정합성 게이트

- [ ] transfer·journal commit과 outbox 의도가 같은 transaction에 있습니다.
- [ ] rollback된 거래의 event가 publish되지 않습니다.
- [ ] publisher crash 뒤 outbox가 재처리됩니다.
- [ ] duplicate delivery에도 consumer side effect가 중복되지 않습니다.
- [ ] poison event가 retry 상한 뒤 DLQ로 격리됩니다.
- [ ] replay가 원본 event ID와 실행 기록을 보존합니다.
- [ ] 지원하지 않는 schema version이 명시적으로 처리됩니다.

## 측정·증거 게이트

- [ ] direct publish baseline의 crash window를 실제 재현했습니다.
- [ ] commit outbox·publish·consume·side effect 수가 서로 대조됩니다.
- [ ] duplicate delivery와 duplicate suppression 수가 원본에 있습니다.
- [ ] delivery lag의 시작·종료·단위·백분위·분모가 기록돼 있습니다.
- [ ] recovery duration과 backlog 0 기준이 기록돼 있습니다.
- [ ] DLQ·replay 명령과 결과가 재현됩니다.
- [ ] event schema fixture와 compatibility test가 있습니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                                  |
| --------------- | ---------------------------------------------------------- |
| 기술적 의사결정 | 직접 publish 실패와 outbox 선택, polling·중복·cleanup 비용 |
| 문제 해결       | 두 crash window, duplicate, poison, ordering 복구 과정     |
| 성과·임팩트     | event·side effect 수 대조와 lag·recovery 원본              |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] 계획에만 있는 CDC를 실제 사용 스택에 넣지 않았습니다.
- [ ] 대표 수치와 diagram에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] outbox·broker를 exactly-once 또는 SPOF 제거로 표현하지 않았습니다.
- [ ] publish ack latency와 end-to-end delivery lag를 구분했습니다.
- [ ] 평균과 p95·p99를 바꿔 쓰지 않았습니다.
- [ ] replay 성공을 자동 복구 완료와 혼동하지 않았습니다.
- [ ] 중복 억제 수와 고유 side effect 수의 분모가 일치합니다.
- [ ] CDC·choreography 등 구현하지 않은 용어를 사용하지 않았습니다.

## 면접 방어

- [ ] publisher·consumer 각각의 crash window를 설명할 수 있습니다.
- [ ] inbox와 side effect transaction 경계를 설명할 수 있습니다.
- [ ] outbox table 증가와 cleanup 정책 비용을 설명할 수 있습니다.
- [ ] ordering 보장이 필요한 event와 필요 없는 event를 구분할 수 있습니다.
- [ ] DLQ 운영자가 확인해야 할 정보를 말할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록        |
| ---------------- | ---------------------------- |
| Portfolio Review | PASS 또는 REJECT             |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수 |
| 전달 의미        | 실제 보장과 미보장           |
| Evidence         | commit SHA와 manifest 경로   |
| 최종 상태        | RELEASED 또는 HOLD           |

commit된 발행 의도가 유실되거나 side effect가 중복되거나 🔴가 남으면 태그를 생성하지 않습니다.
