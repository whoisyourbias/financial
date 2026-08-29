# 프로젝트 01 — 결제 계약과 불변 결제 원장

## 목표

토스페이먼츠 V2 공개 문서에서 선택한 결제 승인 계약을 읽고, 그 계약을 지탱하는 결제 집계와 복식부기 원장을 구현합니다. 외부 계약과 내부 회계 모델을 분리해 이후 프로젝트의 기준선을 만듭니다.

## 범위

- 샌드박스 전용 결제 세션 생성: `POST /sandbox/v1/payment-sessions`
- 결제 승인: `POST /v1/payments/confirm`
- paymentKey 조회: `GET /v1/payments/{paymentKey}`
- orderId 조회: `GET /v1/payments/orders/{orderId}`
- Payment 응답의 선택 필드와 상태
- 승인 시 원자적으로 생성되는 불변 분개와 잔액 검증
- 최소 샘플 상점의 세션 생성 → 승인 → 조회 흐름

계약 ID는 `TP-PAY-001`, `TP-PAY-002`, `TP-PAY-003`, `TP-WIRE-001..006`, `TP-STATE-001`, `SBX-001`을 따릅니다.

## 비범위

- 실제 토스페이먼츠, 카드사, 은행 네트워크 호출
- 실제 API 키나 고객 데이터
- 취소, 웹훅, 가상계좌, 정산
- 공개 문서 전체에 대한 완전 호환 주장

## 핵심 모델

- `Merchant`: 합성 MID의 소유자
- `PaymentSession`: 승인 전에 샌드박스가 만든 paymentKey/orderId/amount의 기대값
- `Payment`: 외부 응답 계약을 만드는 결제 집계
- `JournalEntry`와 `Posting`: 승인 사건의 불변 회계 기록
- `AccountBalance`: 분개 합계로 검증 가능한 파생 상태

금액은 정수 최소 화폐 단위 또는 `BigDecimal`로만 표현하고 `double`과 `float`를 사용하지 않습니다.

## 불변식

1. 승인 요청의 paymentKey, orderId, amount는 같은 결제 세션과 일치해야 합니다.
2. 한 MID 안에서 orderId와 paymentKey는 각각 유일합니다.
3. 승인 상태 전이와 대응 분개는 한 트랜잭션에서 커밋됩니다.
4. 분개 차변 합계와 대변 합계는 항상 같습니다.
5. 게시된 분개는 수정·삭제하지 않고 역분개로만 보정합니다.
6. 응답에 없는 필드를 임의로 채워 공개 계약 호환처럼 보이게 하지 않습니다.

## 구현 순서

1. 계약 매트릭스의 필드·오류·상태를 테스트 픽스처로 고정합니다.
2. 결제 세션과 Payment 집계를 구현합니다.
3. 승인 유스케이스와 복식부기 분개를 같은 트랜잭션으로 묶습니다.
4. 두 조회 API와 선택 응답 필드 직렬화를 구현합니다.
5. 샘플 상점 흐름과 OpenAPI 예시를 추가합니다.
6. 정상·오류·트랜잭션 롤백 통합 테스트를 작성합니다.

## 의사결정 기록 후보

- 정수 최소 화폐 단위와 `BigDecimal` 중 선택 근거
- 결제 집계와 원장 집계를 분리한 이유와 일관성 비용
- 계산 잔액과 저장 잔액의 비교
- 공개 Payment 응답과 내부 모델 사이 어댑터 경계

실제로 비교·실험한 대안만 ADR에 기록합니다.

## 검증 산출물

- 계약 테스트와 OpenAPI 스냅샷
- 정상 승인·중복 승인·금액 불일치 통합 테스트
- 분개 균형 속성 테스트
- 트랜잭션 실패 시 Payment와 원장이 함께 롤백되는 테스트
- 샘플 상점 실행 기록
- `evidence/projects/01/` manifest와 재현 명령

## 근거

- `docs/TOSS_PAYMENTS_CONTRACT_MATRIX.md`
- `docs/TOSS_PAYMENTS_EVIDENCE_MAP.md`
- source refs: `toss-payments-core-api`, `toss-payments-online-payment`, `toss-payments-request-response`, `toss-payments-data-types`, `toss-payments-enum-codes`
- 사례 연구: `toss-legacy-ledger`는 설계 질문을 얻는 용도이며 계약 근거가 아닙니다.
