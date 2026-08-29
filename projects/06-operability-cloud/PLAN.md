# 프로젝트 06 — 결제 샌드박스 운영성과 클라우드 배포

## 목표

프로젝트 01–05의 핵심 결제 경로를 관측·복구할 수 있게 만들고, 합성 트래픽으로 재현 가능한 클라우드 쇼케이스를 배포합니다.

## 핵심 사용자 여정

샘플 상점 세션 생성 → 승인 → 조회 → 부분 취소 → 웹훅 수신과, 가상계좌 발급 → 입금 → 콜백 흐름을 운영 기준선으로 삼습니다.

## 범위

- 구조화 로그, metrics, traces와 correlation ID
- 결제 승인·취소·웹훅·대사 SLI
- 대시보드와 경보
- readiness/liveness와 graceful shutdown
- 백업·복구·롤백 런북
- IaC 기반 비프로덕션 배포
- `showcase-01-payment-sandbox` 태그와 재현 manifest

## 불변식

1. 관측 데이터에 API 키, 개인정보, 전체 결제 식별자를 노출하지 않습니다.
2. 성공률과 지연의 분모·구간·백분위를 대시보드와 보고서에 명시합니다.
3. readiness는 의존성 장애에서 실제 처리 가능 상태를 반영합니다.
4. 배포 실패 시 이전 버전과 DB 호환 경로로 되돌릴 수 있습니다.
5. “고가용성”, “대용량”, “무중단”은 검증한 조건과 범위를 함께 적습니다.
6. 비용 상한과 리소스 삭제 절차를 배포 전에 정의합니다.

## 필수 SLI

- 승인·취소 결과별 처리량과 p50/p95/p99
- 멱등 재시도와 동시성 충돌률
- webhook pending age, retry count, exhausted count
- 가상계좌 미대사 건수와 batch lag
- DB pool, lock wait, CPU, memory
- 샘플 상점 end-to-end 성공률

목표값은 첫 기준 측정 뒤 기록하며 임의의 개선률을 선기입하지 않습니다.

## 구현 순서

1. SLI 사전과 correlation 규칙을 고정합니다.
2. OpenTelemetry 기반 trace와 metrics를 연결합니다.
3. 대시보드·경보·장애 주입 시나리오를 작성합니다.
4. IaC와 배포·롤백 파이프라인을 구현합니다.
5. 부하·복구·비용 측정을 수행합니다.
6. 재현 명령, 원시 결과, 대시보드 캡처를 manifest에 묶습니다.

## 검증 산출물

- 정상·장애 trace 샘플
- metric 정의와 dashboard JSON
- 장애 주입 및 복구 타임라인
- 배포·롤백·백업 복구 기록
- 비용 추정과 삭제 확인
- `evidence/projects/06/` 및 쇼케이스 manifest

## 근거

- source refs: `toss-payments-status-page`, `toss-payments-deploy-checklist`, `toss-payments-environment`
- 공개 상태 페이지의 운영 현상을 참고하되 토스페이먼츠 내부 SLO를 추정하지 않습니다.
