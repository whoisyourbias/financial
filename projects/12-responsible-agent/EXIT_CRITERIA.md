# 프로젝트 12 종료 기준

## 권한·기능 게이트

- [ ] Agent credential에 APPROVE·EXECUTE 권한이 없습니다.
- [ ] read tool이 사용자·역할의 리소스 scope를 강제합니다.
- [ ] Agent는 금전 변경 대신 versioned proposal만 생성합니다.
- [ ] approval이 proposal version·payload hash·expiry에 묶입니다.
- [ ] execution이 현재 상태·금액·승인·idempotency를 재검증합니다.
- [ ] duplicate·expired·tampered proposal이 실행되지 않습니다.
- [ ] model failure가 승인·실행 우회로 이어지지 않습니다.

## 공격·증거 게이트

- [ ] 정상·적대·권한 scenario corpus가 versioned돼 있습니다.
- [ ] expected tool·proposal·refusal이 scenario별로 정의돼 있습니다.
- [ ] prompt·document·tool injection 결과가 영속 상태와 대조됩니다.
- [ ] 무단 persistent mutation 수가 고정 corpus에서 확인됩니다.
- [ ] model→proposal→approval→execution→ledger audit가 연결됩니다.
- [ ] approval API와 Agent가 다른 authentication boundary를 사용합니다.
- [ ] 금융 AI 원칙별 구현 통제와 residual risk가 문서화됐습니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                                         |
| --------------- | ----------------------------------------------------------------- |
| 기술적 의사결정 | READ·PROPOSE·APPROVE·EXECUTE 분리와 새 UX·운영 비용               |
| 문제 해결       | injection, 권한 우회, 승인 tamper·expiry·replay, stale state 처리 |
| 성과·임팩트     | 고정 corpus의 tool·proposal·영속 상태·audit 원본 정합             |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] AI 응답과 실제 실행 결과를 구분했습니다.
- [ ] 대표 Agent 주장에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] human-in-the-loop를 단순 UI 확인으로 표현하지 않았습니다.
- [ ] 모델 refusal을 유일한 보안 통제로 사용하지 않았습니다.
- [ ] 고정 corpus의 0건을 모든 공격 방어로 일반화하지 않았습니다.
- [ ] Agent를 자율 금융 의사결정자로 표현하지 않았습니다.
- [ ] chain-of-thought를 저장·공개 evidence로 요구하지 않았습니다.
- [ ] proposal latency와 최종 승인·실행 완료시간을 구분했습니다.

## 면접 방어

- [ ] Agent와 실행 서비스의 credential·trust boundary를 설명할 수 있습니다.
- [ ] proposal hash·version·expiry가 막는 공격을 설명할 수 있습니다.
- [ ] 승인 뒤 stale state 재검증과 실패 결과를 설명할 수 있습니다.
- [ ] prompt·document·tool injection 차이를 설명할 수 있습니다.
- [ ] 평가 corpus가 보장하지 못하는 residual risk를 말할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록              |
| ---------------- | ---------------------------------- |
| Portfolio Review | PASS 또는 REJECT                   |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수       |
| 공격 corpus      | scenario·model·prompt·tool version |
| Evidence         | commit SHA와 manifest 경로         |
| 최종 상태        | RELEASED 또는 HOLD                 |

Agent가 승인 없이 영속 변경을 만들거나 tampered·expired proposal이 실행되거나 audit가 끊기거나 🔴가 남으면 태그를 생성하지 않습니다.
