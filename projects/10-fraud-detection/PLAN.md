# 프로젝트 10 — 결제 이상거래 탐지

## 목표

합성 결제·취소·가상계좌 이벤트로 설명 가능한 위험 점수를 만들고, 정책·모델 버전·결정 근거·오탐 비용을 운영자가 검토할 수 있게 합니다.

## 선행 조건

프로젝트 02, 03, 05, 06의 결제 사건, MID 격리, 관측 기준선을 사용합니다.

## 범위

- `payment.status.changed`, 취소, 입금 사건의 feature projection
- 규칙 기반 baseline과 경량 모델 후보
- 실시간 위험 점수와 `ALLOW/REVIEW/BLOCK` 제안
- reason code, feature snapshot, 정책·모델 version
- drift와 데이터 품질 모니터링
- 사람이 승인하는 review queue
- 합성 공격·정상 시나리오

프로젝트 10의 결정은 기본적으로 조언이며 실제 결제 차단 연결은 별도 gate 뒤에서만 수행합니다.

## 특징 후보

- MID·결제수단별 금액 편차
- 짧은 시간의 승인 실패·재시도·취소 velocity
- 신규 상점 또는 신규 합성 구매자 구간
- 가상계좌 발급-입금 시간
- 기기·IP의 비식별 범주
- 웹훅·대사 이상 신호

개인정보 원문, 실제 카드번호, 실제 고객 행동 데이터는 사용하지 않습니다.

## 불변식

1. feature는 event time과 model time을 구분합니다.
2. 결정마다 입력 feature snapshot과 버전을 보존합니다.
3. 같은 입력·버전은 같은 결과를 만듭니다.
4. 학습·평가 분할에서 미래 정보 누수를 막습니다.
5. precision/recall만 아니라 review volume과 오탐 비용을 함께 제시합니다.
6. 실제 효과로 표현하는 수치는 재현 가능한 합성 데이터 범위로 한정합니다.

## 구현 순서

1. 위협 가설과 합성 데이터 생성 규칙을 문서화합니다.
2. 규칙 baseline과 feature pipeline을 구현합니다.
3. 시간 기준 train/validation/test 분할을 만듭니다.
4. 모델 후보를 baseline과 비교합니다.
5. 설명·review queue·override 감사 로그를 구현합니다.
6. drift·품질 경보와 rollback을 검증합니다.

## 검증 산출물

- 데이터 카드와 seed
- leakage 검사
- confusion matrix, PR curve, threshold별 비용표
- reason code 일치 테스트
- drift·rollback 시나리오
- `evidence/projects/10/` manifest

## 근거

- 결제 계약은 `docs/TOSS_PAYMENTS_CONTRACT_MATRIX.md`를 따릅니다.
- FDS 정책·모델은 저장소의 합성 설계이며 토스페이먼츠 내부 FDS를 재현한 것이 아닙니다.
