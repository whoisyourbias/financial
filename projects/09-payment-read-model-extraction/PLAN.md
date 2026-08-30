# 프로젝트 09 — 결제 조회 모델과 서비스 경계

## 목표

결제 상태의 권위 원본인 Payment command model과 회계 사실의 권위 원본인 ledger journal을 유지하면서 상점 조회용 read model을 분리하고, 단일 프로세스 projection과 별도 프로세스 추출을 같은 워크로드로 비교합니다.

## 필수 범위

- 버전이 있는 결제 승인·취소 projection event 고정 세트
- PostgreSQL 기반 상점 결제 조회 모델
- paymentKey·orderId 조회 두 use case와 필요한 필드
- 멱등 upsert, 순서 역전, 누락 탐지, 재구축
- 동일 프로세스와 별도 프로세스 두 배치
- lag·정합성·운영 복잡도 비교
- 서비스 추출 여부를 측정 후 결정

## 확장 범위

- 입금·정산 projection event
- 장애 시 command-side 조회 fallback
- 검색 엔진·분석 DB와 추가 조회 use case

두 조회 use case, 승인·취소 event set, 중복·역순·재구축과 두 배치 비교가 끝나기 전에는 확장을 시작하지 않습니다. 검색 엔진이나 분석 DB는 별도 요구사항과 측정 근거가 생길 때만 추가합니다.

## 불변식

1. Payment command model은 결제 상태 전이의 권위 원본이고 ledger journal은 회계 사실의 권위 원본입니다. read model은 어느 쪽도 대체하지 않습니다.
2. read model 장애가 승인·취소 커밋을 막지 않습니다.
3. 같은 event ID·version의 재처리는 결과를 바꾸지 않습니다.
4. 오래된 version이 최신 projection을 덮어쓰지 않습니다.
5. 재구축 결과는 같은 source checkpoint에서 기존 결과와 의미상 같습니다.
6. 별도 서비스 추출을 “MSA 전환”이나 성능 개선으로 선결론 내리지 않습니다.

## 구현 순서

1. 조회 사용 사례와 필요한 필드를 먼저 측정·정의합니다.
2. projection event schema와 진화 규칙을 고정합니다.
3. 동일 프로세스 projection을 기준선으로 구현합니다.
4. 누락·역전·중복·재구축 도구를 구현합니다.
5. 같은 데이터와 부하로 별도 프로세스를 비교합니다.
6. 지연, 장애 격리, 배포·운영 비용을 ADR에 기록하고 경계를 결정합니다.

## 비교 지표

- projection lag와 catch-up time
- 조회 p50/p95/p99
- 명령 경로 영향
- 불일치 건수와 재구축 시간
- 배포·장애 복구 단계 수
- 인프라 비용

## 검증 산출물

- event schema compatibility 테스트
- 중복·역전·누락 failure injection
- checkpoint 기반 rebuild 체크섬
- 두 배치의 동일 워크로드 보고서
- 추출 또는 유지 결정 ADR
- `projects/09-payment-read-model-extraction/evidence/` manifest

## 근거

- source refs: `toss-payments-core-api`, `toss-payments-request-response`
- 사례 연구: `toss-legacy-data-serving`, `toss-legacy-infrastructure`에서 읽기 경계 질문을 얻되 토스 내부 아키텍처를 추정하지 않습니다.
