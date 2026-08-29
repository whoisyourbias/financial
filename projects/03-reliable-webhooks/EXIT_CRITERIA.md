# 프로젝트 03 종료 기준 — 신뢰성 있는 웹훅

## 계약과 전달

- [ ] `PAYMENT_STATUS_CHANGED`와 `CANCEL_STATUS_CHANGED` payload 계약 테스트가 있다.
- [ ] HTTP 200·10초 이내 성공 판정이 가짜 시계 테스트로 고정된다.
- [ ] 공개 문서의 7회 재전송 간격과 종료 상태가 자동 검증된다.
- [ ] 결제 상태 변경과 outbox 기록이 한 트랜잭션이다.
- [ ] 샘플 상점 inbox가 중복 이벤트 부작용을 한 번으로 제한한다.

## 장애와 운영

- [ ] 500, 429, 연결 거부, 타임아웃, 프로세스 종료가 주입된다.
- [ ] 재시작 후 미전송 이벤트가 유실 없이 이어진다.
- [ ] 수동 재전송이 원래 이력을 보존한다.
- [ ] pending age, retry count, exhausted count가 관측된다.
- [ ] 브로커·outbox 내부 포맷을 공개 토스 계약으로 표현하지 않는다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
