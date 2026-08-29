# 프로젝트 05 종료 기준 — 상점 API 보안과 감사

## 인증과 격리

- [ ] HTTP Basic 인증의 선택 계약과 오류가 테스트된다.
- [ ] 원문 시크릿은 발급 응답 이후 저장·로그되지 않는다.
- [ ] 모든 결제·웹훅·정산 조회가 MID와 환경으로 격리된다.
- [ ] 타 MID의 paymentKey/orderId 추측 접근이 거절된다.
- [ ] 키 회전·유예·즉시 폐기가 캐시 포함 시나리오에서 검증된다.

## 권한과 감사

- [ ] 역할별 허용·거부 matrix가 자동 테스트된다.
- [ ] 거부와 민감 변경에 actor, target, reason, trace가 남는다.
- [ ] 감사 기록 변조 탐지 또는 append-only 보장이 검증된다.
- [ ] secret/PII log scan과 의존성·정적 분석 결과가 보존된다.
- [ ] live-simulated가 실제 라이브 망으로 오해되지 않게 표시된다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
