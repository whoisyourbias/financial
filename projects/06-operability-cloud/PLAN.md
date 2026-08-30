# 프로젝트 06 — 결제 샌드박스 운영성과 클라우드 배포

## 목표

프로젝트 01–05의 핵심 결제 경로를 관측·복구할 수 있게 만들고, 합성 트래픽으로 재현 가능한 클라우드 쇼케이스를 배포합니다.

## 핵심 사용자 여정

샘플 상점 세션 생성 → 승인 → 조회 → 부분 취소 → 웹훅 수신과, 가상계좌 발급 → 입금 → 콜백 흐름을 운영 기준선으로 삼습니다.

## 필수 범위

- 구조화 로그, metrics, traces와 correlation ID
- 두 핵심 사용자 여정과 atomic SLI 6개
- 한 대시보드와 두 장애 경보
- readiness/liveness와 graceful shutdown
- 배포·롤백 런북
- 하나의 IaC 비프로덕션 candidate 배포와 사전 기록한 baseline container digest로 수동 롤백
- manifest `intendedTags`에 `p06-operability-cloud`, `showcase-01-payment-sandbox` 준비; review·release candidate 뒤 공통 RELEASED 게이트에서 tag 생성

## 불변식

1. 관측 데이터에 API 키, 개인정보, 전체 결제 식별자를 노출하지 않습니다.
2. 성공률과 지연의 분모·구간·백분위를 대시보드와 보고서에 명시합니다.
3. readiness는 의존성 장애에서 실제 처리 가능 상태를 반영합니다.
4. candidate 배포 전 baseline container digest와 DB schema 호환 범위를 기록하고, 실패 시 그 immutable artifact로 되돌립니다. destructive migration은 이번 필수 범위에서 금지합니다.
5. “고가용성”, “대용량”, “무중단”은 검증한 조건과 범위를 함께 적습니다.
6. 비용 상한과 리소스 삭제 절차를 배포 전에 정의합니다.

## 필수 atomic SLI 6개

| ID | 정의 |
| --- | --- |
| `SLI-CARD-SUCCESS` | 카드 여정 완료 수 / 시작 수 |
| `SLI-CARD-P95` | 성공·실패를 모두 포함한 카드 여정 end-to-end p95 ms |
| `SLI-VA-SUCCESS` | 가상계좌 여정 완료 수 / 시작 수 |
| `SLI-VA-P95` | 성공·실패를 모두 포함한 가상계좌 여정 end-to-end p95 ms |
| `SLI-WEBHOOK-AGE` | 아직 성공하지 않은 webhook 중 가장 오래된 age seconds |
| `SLI-RECON-OPEN` | 측정 시점의 미해결 가상계좌 대사 차이 건수 |

목표값은 첫 기준 측정 뒤 기록하며 임의의 개선률을 선기입하지 않습니다.

## 확장 범위

- webhook exhausted count, batch lag, 승인·취소별 p50/p99, 동시성 충돌, DB pool·lock wait·CPU·memory의 추가 대시보드
- 세 번째 이상 장애, 브라우저 운영 UI polish
- backup/restore 훈련과 다중 topology 비교

필수 SLI·두 장애·단일 배포·롤백이 완료되기 전에는 확장 범위를 시작하지 않습니다. 확장 미완료는 release를 막지 않고 limitation에 기록합니다.

## 구현 순서

1. SLI 사전과 correlation 규칙을 고정합니다.
2. OpenTelemetry 기반 trace와 metrics를 연결합니다.
3. 한 대시보드·두 경보·두 장애 주입 시나리오를 작성합니다.
4. IaC와 배포·롤백 파이프라인을 구현합니다.
5. 고정 합성 부하의 필수 SLI·복구·비용 측정을 수행합니다.
6. 재현 명령, 원시 결과, 대시보드 캡처를 manifest에 묶습니다.

## 검증 산출물

- 정상·장애 trace 샘플
- metric 정의와 dashboard JSON
- 장애 주입 및 복구 타임라인
- 배포·롤백 기록
- 비용 추정과 삭제 확인
- `projects/06-operability-cloud/evidence/` manifest의 `intendedTags`에 프로젝트 tag와 쇼케이스 tag 모두 기록하고 release candidate 입력으로 제공

## 근거

- source refs: `toss-payments-status-page`, `toss-payments-deploy-checklist`, `toss-payments-environment`
- 공개 상태 페이지의 운영 현상을 참고하되 토스페이먼츠 내부 SLO를 추정하지 않습니다.
