# 프로젝트 03 — 신뢰성 있는 웹훅 전달

## 목표

결제 상태 변경을 상점 엔드포인트에 최소 한 번 전달하고, 샘플 상점이 중복 수신에도 부작용을 한 번만 적용하도록 만듭니다.

## 필수 범위

- `PAYMENT_STATUS_CHANGED`
- 공식 transmission 헤더와 Payment 상태 변경 body
- 전송 대상과 시도 이력
- 10초 안의 HTTP 200 성공 판정
- 공개 문서의 7회 재전송 간격
- 운영자 수동 재전송
- 샘플 상점 웹훅 inbox
- 내부 outbox와 선택적 브로커 전달

`DEPOSIT_CALLBACK`은 같은 전달 엔진을 사용하되 프로젝트 04에서 도메인 사건을 연결합니다.

## 불변식

1. 결제 상태 변경과 outbox 레코드는 같은 DB 트랜잭션에서 기록합니다.
2. 각 HTTP 요청의 transmission ID·time·retried-count를 별도 수신 attempt로 보존하며 retry 사이 transmission ID가 같다고 가정하지 않습니다.
3. 샘플 상점은 같은 canonical Payment snapshot의 반복 전달에서 business effect를 다시 만들지 않습니다. 이는 `INT-WEBHOOK-001` 내부 정책이며 공식 body 필드로 임의 ID를 추가하지 않습니다.
4. HTTP 200 이외 응답과 10초 초과는 실패 시도로 남깁니다.
5. 자동 재전송은 문서에 명시된 최대 횟수와 간격을 가짜 시계로 검증합니다.
6. payload에는 자격증명과 불필요한 개인정보를 넣지 않습니다.
7. 내부 Kafka 토픽이나 메시지 포맷을 토스페이먼츠 공개 계약으로 표현하지 않습니다.

## 전달 상태

`PENDING → DELIVERING → SUCCEEDED` 또는 `RETRY_WAIT → EXHAUSTED`를 기본으로 합니다. 수동 재전송은 원래 시도 이력을 보존한 새 attempt로 기록합니다.

## 구현 순서

1. 공개 웹훅의 transmission 헤더와 `PAYMENT_STATUS_CHANGED` body 계약 픽스처를 고정합니다.
2. 결제 트랜잭션 outbox를 구현합니다.
3. HTTP dispatcher, 타임아웃, 재전송 스케줄러를 구현합니다.
4. 샘플 상점 inbox의 attempt 이력과 canonical Payment snapshot 중복 방지를 구현합니다.
5. 장애·복구·수동 재전송 API/CLI와 지표를 추가합니다.
6. 브로커 유무에 따른 복구 절차를 문서화합니다.

## 실패 주입

- 상점 500, 429, 연결 거부, 10초 초과
- dispatcher 종료 후 재시작
- DB 커밋 성공 후 프로세스 종료
- 같은 event의 병렬 전달
- 샘플 상점 처리 성공 후 응답 유실

## 검증 산출물

- 이벤트별 계약 테스트
- 가짜 시계 기반 재시도 스케줄 테스트
- outbox/inbox 중복 전달 통합 테스트
- 미전송 건수·재시도 횟수·최종 실패 지표
- 장애 복구 런북과 `projects/03-reliable-webhooks/evidence/` manifest

## 확장 범위

운영 브라우저 화면과 다중 broker topology 비교는 프로젝트 06 이후의 확장입니다. 프로젝트 03의 필수 범위는 계약·API/CLI·지표 재현까지입니다.

## 근거

- source refs: `toss-payments-webhook-guide`, `toss-payments-webhook-events`, `toss-payments-status-page`
- 사례 연구: `toss-legacy-infrastructure`는 운영 질문의 출발점이며 공개 웹훅 규약을 대체하지 않습니다.
