# 프로젝트 04 — 가상계좌 입금과 대사

## 목표

가상계좌 발급·입금·취소 흐름과 `DEPOSIT_CALLBACK`을 재현하고, 내부 결제 상태·원장·웹훅 전달 기록의 차이를 검출하고 복구합니다.

## 범위

- `POST /v1/virtual-accounts`
- 샌드박스 입금 주입: `POST /sandbox/v1/virtual-accounts/{paymentKey}/deposits`
- `WAITING_FOR_DEPOSIT → DONE`
- `DEPOSIT_CALLBACK`
- 입금 전 취소와 입금 후 취소 규칙
- 합성 은행 스냅샷을 이용한 정기 대사
- Spring Batch 기반 재시작 가능한 대사 작업

## 불변식

1. 가상계좌 발급은 같은 MID 안의 orderId와 amount에 묶입니다.
2. 정확한 금액의 유효 입금만 결제를 완료합니다.
3. 입금 상태, 원장 분개, 웹훅 outbox는 한 트랜잭션에서 기록합니다.
4. 같은 입금 참조번호를 재수신해도 분개와 웹훅 사건은 하나입니다.
5. 입금 전 취소와 입금 후 환불은 서로 다른 규칙으로 처리합니다.
6. 대사 재실행은 이미 처리된 차이를 중복 보정하지 않습니다.
7. 합성 계좌번호와 입금자명만 사용하며 실제 금융정보를 저장하지 않습니다.

## 대사 분류

- `MATCHED`: 내부와 합성 은행 스냅샷이 일치
- `MISSING_INTERNAL`: 외부 입금은 있으나 내부 완료가 없음
- `MISSING_EXTERNAL`: 내부 완료는 있으나 외부 입금이 없음
- `AMOUNT_MISMATCH`
- `DUPLICATE_REFERENCE`

자동 보정과 사람 검토 대상을 정책으로 분리하고 모든 보정은 원본 기록을 보존합니다.

## 구현 순서

1. 가상계좌 요청·응답과 상태·취소 규칙을 계약 테스트로 고정합니다.
2. 발급과 샌드박스 입금 어댑터를 구현합니다.
3. 입금 완료·원장·웹훅을 원자적으로 연결합니다.
4. 대사 분류기와 재시작 가능한 배치를 구현합니다.
5. 차이 승인·재처리 운영 화면과 런북을 추가합니다.
6. 대량 합성 데이터로 재시작·중복·부분 실패를 검증합니다.

## 검증 산출물

- 가상계좌 계약 테스트
- 입금 중복·금액 불일치·만료 테스트
- 배치 restart/checkpoint 테스트
- 대사 차이 manifest와 보정 감사 로그
- `evidence/projects/04/` 재현 자료

## 근거

- source refs: `toss-payments-virtual-account`, `toss-payments-virtual-account-webhook`, `toss-payments-webhook-guide`, `toss-payments-cancel-guide`
- 사례 연구: `toss-legacy-data-serving`는 대사·데이터 품질 질문을 얻는 참고 자료입니다.
