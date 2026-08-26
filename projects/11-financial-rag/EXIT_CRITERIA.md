# 프로젝트 11 종료 기준

## 기능·데이터 게이트

- [ ] 모든 문서에 source·취득일·version·checksum이 있습니다.
- [ ] 공개 문서와 합성 내부 정책이 구분됩니다.
- [ ] document scope가 retrieval에서 강제됩니다.
- [ ] answerable 질문은 인용을, unanswerable 질문은 명시적 거절을 반환합니다.
- [ ] 응답에 corpus·model·prompt version이 있습니다.
- [ ] API·local model timeout·fallback이 정책대로 동작합니다.
- [ ] prompt·document injection과 PII 유도가 평가됩니다.

## 평가·증거 게이트

- [ ] golden question의 expected document·location이 사람이 확인됐습니다.
- [ ] retrieval Recall@K와 final citation·answer 평가가 분리돼 있습니다.
- [ ] API/local 비교가 같은 corpus·질문·context 조건을 사용합니다.
- [ ] 조건이 다르면 직접 비교 제한을 명시했습니다.
- [ ] latency의 retrieval·model·end-to-end 구간이 분리돼 있습니다.
- [ ] token·비용 계산식과 model version이 기록돼 있습니다.
- [ ] LLM evaluator와 수동 판정의 disagreement가 공개돼 있습니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                                       |
| --------------- | --------------------------------------------------------------- |
| 기술적 의사결정 | retrieval·model routing 후보 비교와 보안·비용·지연 비용         |
| 문제 해결       | 무근거 답변, scope, injection, timeout, version regression 처리 |
| 성과·임팩트     | golden set의 retrieval·citation·refusal·수동 평가 원본          |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] corpus·질문·결과·모델 버전이 연결됩니다.
- [ ] 대표 RAG 주장에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] LLM-as-a-judge만으로 정확도를 주장하지 않았습니다.
- [ ] retrieval Recall@K와 answer correctness를 바꿔 쓰지 않았습니다.
- [ ] 답변 latency와 retrieval latency를 바꿔 쓰지 않았습니다.
- [ ] model의 자기평가 숫자를 confidence로 표현하지 않았습니다.
- [ ] 문서 snapshot을 최신 법률·자문으로 표현하지 않았습니다.
- [ ] API/local 모델의 다른 조건을 직접 성능 비교하지 않았습니다.

## 면접 방어

- [ ] retrieval과 generation 실패를 분리해 설명할 수 있습니다.
- [ ] golden set 생성·검증과 test 오염 방어를 설명할 수 있습니다.
- [ ] model routing의 비용·보안·지연 tradeoff를 설명할 수 있습니다.
- [ ] prompt·document injection 방어와 남은 한계를 설명할 수 있습니다.
- [ ] unanswerable 거절 기준과 false refusal 비용을 설명할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록                |
| ---------------- | ------------------------------------ |
| Portfolio Review | PASS 또는 REJECT                     |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수         |
| 평가 corpus      | corpus·question·model·prompt version |
| Evidence         | commit SHA와 manifest 경로           |
| 최종 상태        | RELEASED 또는 HOLD                   |

scope 우회, 근거 없는 답변의 성공 처리, 평가 순환, 버전 불명 또는 🔴가 남으면 태그를 생성하지 않습니다.
