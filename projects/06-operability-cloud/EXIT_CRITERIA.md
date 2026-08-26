# 프로젝트 06 종료 기준

## 기능·운영 게이트

- [ ] HTTP·DB·event·batch가 correlation ID로 연결됩니다.
- [ ] SLI 이름·단위·분모·측정점이 정의돼 있습니다.
- [ ] dashboard가 세 가지 핵심 운영 질문에 답합니다.
- [ ] broker·DB·batch 장애가 사전 hypothesis대로 주입됩니다.
- [ ] 각 장애 뒤 금융 불변식과 backlog·batch 결과가 재검증됩니다.
- [ ] AWS ECS/RDS 환경이 Terraform으로 생성·제거됩니다.
- [ ] 관리자 UI가 합성 데이터와 인증된 API만 사용합니다.

## 측정·증거 게이트

- [ ] workload, warm-up, duration, request mix, 환경이 기록돼 있습니다.
- [ ] latency p50/p95/p99의 분모와 오류 포함 여부가 기록돼 있습니다.
- [ ] 장애 탐지·완화·회복 timestamp의 기준 사건이 있습니다.
- [ ] dashboard 숫자와 raw export가 일치합니다.
- [ ] local과 AWS 결과가 별도 표로 구분됩니다.
- [ ] Terraform destroy와 resource 0 확인이 있습니다.
- [ ] AWS 비용 범위·조회 시각이 있습니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                    |
| --------------- | -------------------------------------------- |
| 기술적 의사결정 | SLI/SLO·readiness·cloud topology 선택과 비용 |
| 문제 해결       | 세 장애의 탐지→진단→완화→정합성 재검증       |
| 성과·임팩트     | 조건이 명시된 latency·lag·recovery·cost 원본 |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] dashboard·본문·raw 값과 시간 범위가 일치합니다.
- [ ] 대표 운영 주장에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] 클라우드 데모를 실제 운영·on-call·고가용성 경험으로 표현하지 않았습니다.
- [ ] 합성 workload를 실제 사용자 트래픽으로 표현하지 않았습니다.
- [ ] enqueue·API latency와 end-to-end completion을 구분했습니다.
- [ ] 평균·p95·p99·peak throughput을 바꿔 쓰지 않았습니다.
- [ ] local과 AWS의 다른 topology를 직접 비교하지 않았습니다.
- [ ] alert 발생을 복구 완료나 장애 예방으로 과장하지 않았습니다.

## 면접 방어

- [ ] SLI·SLO·alert의 차이를 설명할 수 있습니다.
- [ ] 세 장애의 timeline과 복구 기준을 설명할 수 있습니다.
- [ ] readiness 의존성이 장애를 전파할 수 있는 경우를 말할 수 있습니다.
- [ ] AWS 비용과 topology tradeoff를 설명할 수 있습니다.
- [ ] 실제 운영 경험과 이 실험의 차이를 명확히 말할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록        |
| ---------------- | ---------------------------- |
| Portfolio Review | PASS 또는 REJECT             |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수 |
| Cloud cleanup    | 제거 시각과 비용 확인        |
| Evidence         | commit SHA와 manifest 경로   |
| 최종 상태        | RELEASED 또는 HOLD           |

AWS 리소스가 남아 있거나 불변식이 실패하거나 dashboard와 원본이 어긋나거나 🔴가 남으면 태그를 생성하지 않습니다.
