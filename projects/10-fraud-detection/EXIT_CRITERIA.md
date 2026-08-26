# 프로젝트 10 종료 기준

## 데이터·기능 게이트

- [ ] 합성 데이터가 seed·schema·version으로 재현됩니다.
- [ ] train·validation·test가 정의한 주체·시간 경계를 지킵니다.
- [ ] feature leakage 검사가 자동화돼 있습니다.
- [ ] 규칙 baseline과 모델이 같은 test sample을 평가합니다.
- [ ] FDS 실패가 거래 commit을 막지 않고 미평가 backlog를 남깁니다.
- [ ] duplicate event가 중복 case를 만들지 않습니다.
- [ ] 분석가 review와 AI score가 분리돼 audit됩니다.

## 평가·증거 게이트

- [ ] class 분포와 confusion matrix 원본이 있습니다.
- [ ] precision·recall·FPR을 raw prediction에서 재계산할 수 있습니다.
- [ ] threshold별 review volume과 비용 tradeoff가 있습니다.
- [ ] rule·model·feature·dataset version이 기록돼 있습니다.
- [ ] model timeout·invalid response·backlog 복구 evidence가 있습니다.
- [ ] 합성 이상 pattern과 현실 일반화 한계가 data/model card에 있습니다.
- [ ] accuracy 단일 숫자로 결과를 요약하지 않았습니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                                       |
| --------------- | --------------------------------------------------------------- |
| 기술적 의사결정 | 규칙 baseline·모델·threshold 선택과 오탐·검토 비용              |
| 문제 해결       | leakage, imbalance, duplicate, model failure, human review 처리 |
| 성과·임팩트     | 고정 test set의 raw prediction·metric·case audit 정합           |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] 데이터·모델·결과의 버전이 서로 연결됩니다.
- [ ] 대표 AI 주장에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] 합성 데이터 성능을 실제 사기 탐지율로 표현하지 않았습니다.
- [ ] accuracy와 precision·recall·FPR을 바꿔 쓰지 않았습니다.
- [ ] test set을 threshold·model tuning에 재사용하지 않았습니다.
- [ ] model score를 신뢰도 확률로 표현하지 않았습니다.
- [ ] 규칙과 모델이 다른 sample·분포를 사용하지 않았습니다.
- [ ] AI 도입이 fraud loss를 줄였다고 인과 주장하지 않았습니다.

## 면접 방어

- [ ] 합성 데이터가 증명하는 것과 증명하지 못하는 것을 설명할 수 있습니다.
- [ ] split과 leakage 방어를 sample ID로 설명할 수 있습니다.
- [ ] threshold 변경이 recall·FPR·review volume에 미치는 영향을 설명할 수 있습니다.
- [ ] 규칙 baseline을 유지한 이유를 설명할 수 있습니다.
- [ ] model failure·backlog·human override 흐름을 설명할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록        |
| ---------------- | ---------------------------- |
| Portfolio Review | PASS 또는 REJECT             |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수 |
| 평가 corpus      | dataset·split·model version  |
| Evidence         | commit SHA와 manifest 경로   |
| 최종 상태        | RELEASED 또는 HOLD           |

데이터 누출, metric 분모 불일치, AI 자동 차단, 현실 일반화 또는 🔴가 남으면 태그를 생성하지 않습니다.
