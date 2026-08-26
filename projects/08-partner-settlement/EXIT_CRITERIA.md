# 프로젝트 08 종료 기준

## 기능·정합성 게이트

- [ ] timeout 뒤 결제가 임의 실패·성공으로 확정되지 않습니다.
- [ ] retry 상한과 전체 시간 budget이 강제됩니다.
- [ ] 유효하지 않거나 오래된 webhook이 상태를 변경하지 않습니다.
- [ ] duplicate·out-of-order webhook이 상태를 중복·역행시키지 않습니다.
- [ ] partner reference와 operation이 1:1로 대조됩니다.
- [ ] clearing 차이가 payment·journal 원본과 함께 분류됩니다.
- [ ] reprocess가 중복 상태·금전 변경을 만들지 않습니다.

## 테스트·증거 게이트

- [ ] stub fault mode와 contract test가 재현됩니다.
- [ ] timeout 뒤 partner·payment·journal timeline이 있습니다.
- [ ] retry attempt·원인·elapsed time 원본이 있습니다.
- [ ] webhook 수신·유효·거절·중복 수의 분모가 일치합니다.
- [ ] signature canonicalization과 replay window가 문서화됐습니다.
- [ ] settlement 개별·합계 보고서가 원본과 일치합니다.
- [ ] 실제 PG 연동이 아니라는 limitation이 공개돼 있습니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                             |
| --------------- | ----------------------------------------------------- |
| 기술적 의사결정 | timeout 확인 전략, retry 위치, webhook 보안 비용      |
| 문제 해결       | 불명확 상태·중복·순서 역전·파트너 outage·정산 차이    |
| 성과·임팩트     | partner attempt·webhook·payment·journal·clearing 대조 |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] 파트너 stub과 실제 경계를 구분했습니다.
- [ ] 대표 외부 연동 주장에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] stub을 실제 PG 연동이나 상용 운영으로 표현하지 않았습니다.
- [ ] webhook ack latency와 payment completion을 구분했습니다.
- [ ] retry 횟수와 고유 operation 수를 구분했습니다.
- [ ] HMAC을 전체 보안·PCI 준수로 표현하지 않았습니다.
- [ ] settlement 차이 탐지를 자동 정산 완료로 표현하지 않았습니다.
- [ ] 여러 retry·rate limit 변경을 하나의 성능 원인으로 귀속하지 않았습니다.

## 면접 방어

- [ ] timeout 뒤 confirmation 상태와 조회 흐름을 설명할 수 있습니다.
- [ ] webhook signature·timestamp·replay 방어를 설명할 수 있습니다.
- [ ] 중첩 retry와 retry storm 위험을 설명할 수 있습니다.
- [ ] 상태 역행 방지와 aggregate version을 설명할 수 있습니다.
- [ ] clearing 차이의 source of truth와 운영 대응을 설명할 수 있습니다.

## 판정 기록

| 항목               | 프로젝트 종료 시 기록        |
| ------------------ | ---------------------------- |
| Portfolio Review   | PASS 또는 REJECT             |
| Red Team           | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수 |
| Partner limitation | stub과 미구현 범위           |
| Evidence           | commit SHA와 manifest 경로   |
| 최종 상태          | RELEASED 또는 HOLD           |

위조·중복 webhook이 상태를 변경하거나 settlement가 원장과 어긋나거나 🔴가 남으면 태그를 생성하지 않습니다.
