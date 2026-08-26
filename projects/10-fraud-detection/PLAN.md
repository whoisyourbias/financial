# 프로젝트 10 — AI 이상거래 탐지와 분석가 검토

| 항목           | 값                                                                 |
| -------------- | ------------------------------------------------------------------ |
| 기간           | 2027-01-04 ~ 2027-01-17                                            |
| 상태           | PLANNED                                                            |
| 선행 프로젝트  | 02, 03, 05, 06                                                     |
| 목표 태그      | `p10-fraud-detection`                                              |
| 주력 채용 신호 | 합성 데이터 설계, AI baseline, 평가, human review, model operation |

## 문제 정의

거래량·빈도·기기·위치·시간 패턴이 평소와 다른 거래를 우선 검토하고 싶지만, 실제 금융사기 데이터는 없고 class imbalance와 오탐 비용도 큽니다. 이 프로젝트는 합성 데이터 안에서 명시적으로 주입한 이상 패턴을 대상으로 규칙 기반선과 이상탐지 모델을 같은 test set에서 비교하고, 결과를 자동 차단이 아니라 분석가 검토 queue로 보냅니다.

이 결과는 실제 사기 탐지율이 아닙니다. 데이터 생성 규칙을 모델이 얼마나 재발견했는지와 AI pipeline의 평가·버전·검토 경계를 보여주는 실험입니다.

## 필수 범위

- seed·schema·version이 있는 합성 거래 generator
- 정상 profile과 명시적 이상 pattern catalog
- train·validation·test의 주체·시간 기준 분리
- 정보 누출 검사
- 규칙 기반 score baseline
- 하나의 해석 가능한 통계·ML 이상탐지 모델
- 같은 test set의 confusion matrix·precision·recall·FPR
- threshold 변화와 review volume tradeoff
- model·feature·dataset version 저장
- 비동기 transaction event consumer
- `UNSCORED`, `SCORED`, `REVIEW_REQUIRED`, `CONFIRMED`, `DISMISSED` case 상태
- 분석가 사유·override·audit

## 비범위

- 실제 금융사기·고객 데이터
- 자동 거래 차단·계정 정지
- 신용평가·대출심사
- 실제 fraud loss 감소 주장
- deep learning 자체 개발
- 모델 결과를 확률적 진실로 표현

## 합성 데이터 계획

최소 다음 feature를 생성합니다.

- transaction amount·currency
- account age·recent transaction count
- hour of day·day of week
- device ID age·IP region category
- merchant category·channel
- prior rolling amount·frequency

이상 pattern 예시:

- 짧은 시간의 급격한 빈도 증가
- 평소 범위를 크게 벗어난 금액
- 새 기기와 낯선 지역의 결합
- 반복된 소액 뒤 큰 금액

test set에는 규칙과 완전히 동일하지 않은 변형 pattern을 일부 포함하되, 실제 현실성을 주장하지 않습니다. 계정 기준 분리로 같은 사용자의 future 정보가 train에 새지 않게 합니다.

## 설계 후보

### 기준선

- 명시적 규칙 점수와 threshold
- 장점: 설명 가능, deterministic
- 비용: 새로운 조합·분포 변화에 취약

### 모델

- 후보: Isolation Forest 또는 동등한 해석 가능한 anomaly model
- Python service가 필요한 경우 OpenAPI contract로 분리
- model을 고급 AI로 포장하지 않고 선택 이유·feature sensitivity를 기록

모델은 규칙 baseline보다 반드시 좋아야 하는 것이 아닙니다. review volume·FPR 비용을 포함해 결과를 해석합니다.

## 데이터 흐름

```text
transaction.completed event
  → feature extraction with version
  → rule baseline + model score
  → threshold policy
  → review case
  → analyst confirm/dismiss + reason
  → audit + evaluation dataset candidate
```

FDS consumer 실패는 거래 commit을 막지 않습니다. 미평가 event를 backlog로 남기고 복구 후 재평가합니다.

## 계획 인터페이스

```text
GET  /api/v1/fds/cases?status=&cursor=
GET  /api/v1/fds/cases/{id}
POST /api/v1/fds/cases/{id}/reviews
GET  /api/v1/fds/models/{version}/evaluations
```

review API는 분석가 역할만 사용하며 모델 score·feature explanation과 사람의 최종 label을 분리합니다.

## 산출물

- synthetic dataset generator·data card
- rule baseline과 model pipeline
- feature·model·dataset registry
- evaluation script와 raw prediction
- analyst review queue·audit
- backlog·model failure fallback
- threshold·한계 ADR

## 10일 작업 계획

| 일차 | 작업                              | 완료 기준                 |
| ---- | --------------------------------- | ------------------------- |
| 1    | use case·비용·이상 pattern·비범위 | 실제 FDS와 차이 명시      |
| 2    | split·leakage·baseline·model ADR  | 평가 protocol 고정        |
| 3    | generator·data card               | seed·분포·version 재현    |
| 4    | feature pipeline·leakage test     | train/test 경계 검증      |
| 5    | rule baseline                     | same test raw prediction  |
| 6    | anomaly model·version             | reproducible train·infer  |
| 7    | case queue·review·audit           | AI와 사람 label 분리      |
| 8    | threshold·failure·backlog 평가    | confusion matrix·FPR 원본 |
| 9    | model card·evidence·한계          | 일반화 금지 문구 포함     |
| 10   | Portfolio Review·Red Team         | PASS와 🔴 0건 또는 HOLD   |

## 테스트와 측정

- 같은 seed의 dataset 재현과 다른 seed의 schema 일치
- account/time 기준 split과 leakage 검사
- feature null·outlier·unknown category
- rule·model 같은 test sample ID 사용
- threshold별 confusion matrix
- precision·recall·FPR의 분모 재계산
- model service timeout·invalid response
- duplicate event와 case 중복
- FDS 중단 뒤 backlog 재평가
- 분석가 권한·중복 review·audit

정확도 하나를 대표 지표로 쓰지 않습니다. class 분포, review 건수, FPR, recall, threshold를 함께 제시합니다.

## 필요한 증거

- generator source·seed·dataset checksum·class 분포
- split 기준과 sample ID leakage 결과
- 규칙·모델 raw prediction
- confusion matrix와 metric 재계산 명령
- threshold별 review volume
- feature·model·dataset version
- case state·analyst outcome·audit 대조
- 합성 데이터와 미지의 현실 pattern limitation

## 예상 면접 질문

1. 본인이 만든 이상 패턴을 모델이 찾은 결과가 무엇을 증명합니까?
2. precision, recall, FPR 중 금융 검토 queue에서 어떤 비용을 우선합니까?
3. 같은 계정 데이터가 train과 test에 섞이면 어떤 누출이 생깁니까?
4. 규칙 baseline보다 모델이 나쁘다면 모델을 유지할 이유가 있나요?
5. FDS 장애 때 거래를 막지 않은 이유와 미평가 거래 복구 방법은 무엇인가요?

## 주요 위험과 다음 회차 연결

- 합성 데이터에서 높은 수치가 나와도 실제 탐지율로 일반화하지 않습니다.
- 분석가 confirmation을 ground truth로 자동 간주하지 않습니다.
- model score를 신뢰도 확률로 표현하지 않습니다.
- 프로젝트 12에서 FDS case를 Agent가 요약할 수 있지만 최종 판정 권한은 사람이 유지합니다.
