# 프로젝트 04 — 대사와 재시작 가능한 배치

| 항목           | 값                                             |
| -------------- | ---------------------------------------------- |
| 기간           | 2026-10-12 ~ 2026-10-25                        |
| 상태           | PLANNED                                        |
| 선행 프로젝트  | 01 원장, 03 이벤트                             |
| 목표 태그      | `p04-reconciliation-batch`                     |
| 주력 채용 신호 | Spring Batch, 정산·대사, 재시작, 데이터 정합성 |

## 문제 정의

원장 기록, 이벤트 전달, 외부 거래 명세는 서로 다른 시점과 실패 경계를 가집니다. 합계만 맞는다고 개별 거래가 올바르게 연결됐다는 보장도 없습니다. 이 프로젝트는 합성 외부 거래 파일과 내부 원장을 개별 식별자·금액·통화·상태·합계 수준에서 비교하고, 중단된 batch를 중복 없이 재시작합니다.

`대사`는 차이를 자동으로 없애는 기능이 아니라 차이를 재현 가능하게 탐지·분류·해결 상태로 관리하는 기능으로 정의합니다.

## 필수 범위

- 버전이 있는 외부 거래 파일 format과 checksum
- cut-off 시간과 영업일 기준
- file ingestion, validation, normalization
- 내부 journal·transfer와 외부 record의 1:1 대사
- missing internal, missing external, amount mismatch, currency mismatch, duplicate 분류
- Spring Batch job·step·execution 상태
- 같은 파일 중복 실행 방어
- 실패한 chunk부터의 안전한 재시작
- 차이 건 조회·수동 해결 상태 기록
- 개별 대사와 합계 대사 보고서

## 비범위

- 실제 은행·PG 정산 파일 연동
- 자동 금전 보정
- 회계 마감과 세무 처리
- 수백만 건 처리 주장
- 운영자 승인 UI

## 대사 불변식

1. 동일 checksum의 파일은 한 비즈니스 결과만 만듭니다.
2. batch 재시작은 이미 확정된 대사 결과를 중복 생성하지 않습니다.
3. 개별 matched record 합계와 보고서 matched 합계가 같습니다.
4. 차이 건은 원본 internal·external 식별자를 보존합니다.
5. 수동 해결은 차이 record를 삭제하지 않고 상태·주체·사유를 추가합니다.
6. cut-off 이후 거래는 현재 batch에 섞이지 않습니다.

## 설계 후보

### Chunk-oriented processing

Spring Batch reader→processor→writer 구조를 사용하고 chunk commit 단위를 실제 실패 시나리오로 검증합니다. chunk 크기는 성능 숫자만으로 선택하지 않고 rollback 범위와 재시작 비용을 함께 봅니다.

### 전체 파일 단일 transaction

단순하지만 파일이 커질수록 lock·rollback 비용이 커집니다. 작은 baseline으로만 비교하며 구현하지 않으면 미실험으로 표시합니다.

### 대사 key

- 외부 reference 단독
- external reference + amount + currency
- 별도 mapping table

키 충돌과 외부 reference 재사용 시나리오를 정의한 뒤 채택합니다.

## 계획 인터페이스

```text
POST /api/v1/reconciliation-files
GET  /api/v1/reconciliation-jobs/{jobId}
GET  /api/v1/reconciliation-jobs/{jobId}/differences
POST /api/v1/reconciliation-differences/{id}/resolutions
```

수동 해결 API는 차이 상태만 변경하며 원장 금액을 직접 수정하지 않습니다.

## 데이터 흐름

```text
versioned file
  → checksum·format validation
  → normalized staging records
  → cut-off snapshot의 internal records 조회
  → individual matching·classification
  → chunk result commit
  → individual·aggregate report
  → unresolved difference queue
```

## 산출물

- 합성 외부 파일 generator와 schema
- Spring Batch job과 restart 설정
- difference taxonomy와 상태 모델
- 중복 파일·부분 실패·재시작 테스트
- 개별·합계 대사 결과
- chunk·대사 key ADR
- 운영 runbook

## 10일 작업 계획

| 일차 | 작업                           | 완료 기준                       |
| ---- | ------------------------------ | ------------------------------- |
| 1    | 대사 정의·차이 유형·cut-off    | 합계와 개별 대사 분리           |
| 2    | file schema·대사 key·chunk ADR | 실패·중복 시나리오 고정         |
| 3    | generator·validation·staging   | checksum·schema reject          |
| 4    | reader·processor·matching      | 차이 유형별 단위 테스트         |
| 5    | writer·job metadata            | chunk 결과 원자적 저장          |
| 6    | duplicate·restart              | 같은 파일·실패 step 재실행 안전 |
| 7    | query·resolution·report        | 원본 식별자와 합계 보존         |
| 8    | 부분 파일·crash·cut-off 실험   | 재시작·대사 원본 생성           |
| 9    | runbook·ADR·evidence           | report와 raw query 연결         |
| 10   | Portfolio Review·Red Team      | PASS와 🔴 0건 또는 HOLD         |

## 테스트와 측정

- 빈 파일, 잘못된 schema, checksum 불일치
- 같은 파일명·다른 checksum, 다른 파일명·같은 checksum
- internal/external 누락과 중복 reference
- amount·currency·status mismatch
- chunk 중간 예외와 프로세스 종료
- restart 뒤 중복 difference·report 생성 여부
- cut-off 직전·직후 거래
- matched·unmatched 합계와 개별 record 재계산

측정은 처리량 과장이 아니라 chunk별 처리 건수, 오류 건수, restart 시작점, 전체 결과 일치에 초점을 둡니다.

## 필요한 증거

- input file·checksum·generator seed
- job instance·execution·step execution query
- crash 지점과 restart 뒤 writer 결과
- difference 유형별 sample과 총합
- 개별 matched 합계와 보고서 합계 대조
- cut-off timezone과 query 조건
- chunk 크기 선택 근거와 한계

## 예상 면접 질문

1. 합계가 맞는데도 개별 대사가 필요한 이유는 무엇인가요?
2. chunk commit 뒤 프로세스가 죽으면 어느 지점부터 재시작합니까?
3. 파일 idempotency를 파일명으로만 판단하면 어떤 문제가 있나요?
4. 수동 해결이 원장 변경과 분리돼야 하는 이유는 무엇인가요?
5. cut-off와 timezone을 잘못 다루면 어떤 거래가 누락·중복됩니까?

## 주요 위험과 다음 회차 연결

- `정산 완료`와 `대사 차이 탐지`를 같은 의미로 쓰지 않습니다.
- 합계만으로 성공을 선언하지 않습니다.
- 작은 fixture 성능을 대용량 batch로 표현하지 않습니다.
- 프로젝트 05는 파일 업로드·차이 조회·수동 해결의 권한과 감사를 추가합니다.
