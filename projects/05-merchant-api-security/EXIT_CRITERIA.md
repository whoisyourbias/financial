# 프로젝트 05 종료 기준 — 상점 API 보안과 감사

## 인증과 격리

- [ ] 공개 Basic wire 형식 `TP-WIRE-001`과 내부 credential 계약 `INT-AUTH-001`의 소유권·테스트가 분리된다.
- [ ] HTTP Basic 인증의 선택 계약과 오류가 테스트된다.
- [ ] 원문 시크릿은 발급 응답 이후 저장·로그되지 않는다.
- [ ] 모든 결제·웹훅·정산 조회가 MID와 환경으로 격리된다.
- [ ] 타 MID의 paymentKey/orderId 추측 접근이 거절된다.
- [ ] 키 회전·유예·즉시 폐기가 캐시 포함 시나리오에서 검증된다.
- [ ] 상점 Basic credential과 사람 운영자 OIDC/JWT가 분리되고 issuer·audience·expiry·role·MID 경계가 테스트된다.

## 권한과 감사

- [ ] 역할별 허용·거부 matrix가 자동 테스트된다.
- [ ] 거부와 민감 변경에 actor, target, reason, trace가 남는다.
- [ ] application API의 append-only 보장과 hash-chain 검증을 테스트하고, application·DB 밖에 고정한 서명 checkpoint 이후의 수정·삭제·순서 변경만 탐지 보장 범위로 명시한다.
- [ ] 첫 checkpoint 전 변조, 서명 키 또는 외부 evidence target 침해는 탐지하지 못한다는 한계를 위협 모델과 결과에 남긴다.
- [ ] secret/PII log scan과 의존성·정적 분석 결과가 보존된다.
- [ ] live-simulated가 실제 라이브 망으로 오해되지 않게 표시된다.
- [ ] rate limit의 주체·구간·한도와 정상·초과·우회 시나리오가 자동 테스트된다.

## 공통 증거 게이트

- [ ] `projects/05-merchant-api-security/evidence/MANIFEST.md`가 `docs/EVIDENCE_POLICY.md`의 공통 manifest 계약과 `evidenceCheck`를 통과한다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 필수 범위는 `./gradlew harnessFull`이 통과한다. 미실행이면 사유를 기록하고 상태를 `HOLD`로 둔다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
