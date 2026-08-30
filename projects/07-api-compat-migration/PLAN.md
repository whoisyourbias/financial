# 프로젝트 07 — API 호환 마이그레이션

## 목표

동일한 선택 공개 계약 뒤에서 취소 상태의 내부 저장 방식을 하나의 구체적 delta로 교체하되, shadow 비교·단일 MID 전환·롤백으로 상점 계약과 금액 불변식을 지킵니다.

## 전제

이 프로젝트의 “레거시”는 학습을 위해 저장소 안에 만든 기준 구현입니다. 토스페이먼츠의 실제 과거 시스템이나 마이그레이션을 재현했다고 주장하지 않습니다.

## 필수 migration delta

기준 엔진의 mutable 취소 누계·상태 갱신을, immutable `PaymentOperation` append와 그 operation에서 계산한 `balanceAmount`·status projection으로 교체합니다. 목적은 동시 부분 취소·재시도의 권위 기록을 한 operation 흐름으로 추적하면서도 승인·조회·취소 공개 응답을 바꾸지 않는 것입니다. 이 delta 이외의 API, 원장, 웹훅 엔진 재작성은 하지 않습니다.

## 필수 범위

- `TP-WIRE-004` additive/파괴적 변경과 구버전 소비자 보호 contract suite
- 승인·조회·취소 공개 계약의 고정 contract suite
- 기준 엔진과 새 엔진의 공통 application port
- 요청·응답 정규화 어댑터
- 읽기 전용 shadow 실행과 차이 분류
- 하나의 합성 MID 수동 canary
- 기존 Payment·Cancel을 operation log로 옮기는 재실행 가능한 backfill과 체크섬
- 수동 롤백과 샘플 상점 무변경 검증

## 확장 범위

- 다중 MID 자동 라우팅과 자동 롤백
- 추가 결제수단·schema migration과 장기 dual-run
- canary 운영 UI

필수 contract·shadow·backfill·단일 canary·수동 rollback이 끝나기 전에는 확장 범위를 시작하지 않습니다.

## 불변식

1. 공개 경로, 인증 방식, 선택 필드, 오류 매핑은 전환 전후 동일합니다.
2. shadow 엔진은 원장·웹훅·외부 응답에 부작용을 만들지 않습니다.
3. 차이는 필드 순서 같은 허용 차이와 금액·상태 같은 차단 차이로 분리합니다.
4. 한 결제 명령은 활성 엔진 한 곳만 권위 있게 커밋합니다.
5. backfill은 재실행 가능하고 원본을 덮어쓰지 않습니다.
6. 롤백 이후에도 새로 생성된 결제를 조회·취소할 호환 경로가 있습니다.

## Go/No-Go와 롤백 기준

- contract suite 실패, `BLOCKING` 또는 `UNKNOWN` diff 1건 이상, shadow 부작용 1건 이상이면 no-go입니다.
- backfill 대상 수·operation 수·금액 체크섬은 100% 일치해야 합니다.
- 성능·오류율 임계치 산정 방법, workload, 표본 단위, 반복 종료 조건과 샘플 상점 SLO에서 도출한 최대 허용 회귀 예산을 baseline 측정 전에 등록합니다.
- 같은 fixture 기준 엔진을 반복 실행해 run-to-run 변동의 단측 95% 신뢰 상한을 구합니다. 그 상한이 최대 허용 회귀 예산보다 작지 않거나 표본이 신뢰구간을 계산하기에 부족하면 candidate 차이를 판별할 수 없으므로 canary는 no-go입니다.
- 최대 허용 회귀 예산을 rollback 임계치로 고정하고 candidate 회귀의 단측 95% 신뢰 상한이 이를 넘으면 즉시 기준 엔진으로 수동 롤백합니다. candidate 측정 뒤 임계치를 바꾸지 않으며 오류율 분자·분모, latency 원본, baseline 반복값과 계산식을 보존합니다.

## 단계

1. 현재 계약과 golden fixture를 기준선으로 고정합니다.
2. 공통 port와 기준 엔진 adapter를 분리합니다.
3. immutable `PaymentOperation` 취소 엔진과 데이터 변환기를 구현합니다.
4. 합성 트래픽을 shadow로 실행해 semantic diff를 수집합니다.
5. 사전 등록한 calibration protocol로 baseline 변동과 rollback 임계치를 고정합니다.
6. 차단 차이 0과 데이터 검증 통과 후 MID 일부를 전환합니다.
7. 오류율·지연·금액 불일치 임계치에서 사전 준비한 기준 엔진으로 수동 롤백합니다.

## 차이 분류

- `IGNORED`: 필드 순서, 생성 시각 허용 오차
- `REVIEW`: 문서상 선택 필드의 표현 차이
- `BLOCKING`: amount, balanceAmount, status, cancelAmount, 오류 코드, 분개 합계
- `UNKNOWN`: 규칙이 없어 사람이 분류해야 하는 차이

## 검증 산출물

- 전환 전후 동일 contract suite 결과
- shadow 무부작용 테스트
- semantic diff 원시 데이터와 분류 규칙
- backfill 재실행·체크섬 보고서
- canary·롤백 타임라인
- baseline 반복 원본, 임계치 계산식과 사전 등록한 calibration protocol
- `projects/07-api-compat-migration/evidence/` manifest

## 근거

- source refs: `toss-payments-versioning`, `toss-payments-request-response`, `toss-payments-error-codes`, `toss-payments-core-api`
- 사례 연구: `toss-legacy-intro`, `toss-legacy-open-api`, `toss-legacy-configuration`에서 질문을 얻되 실제 내부 구조를 모방했다고 표현하지 않습니다.
