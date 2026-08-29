# 프로젝트 02 종료 기준 — 결제 명령과 동시성

## 계약과 기능

- [ ] 승인·두 조회·취소 API의 선택 계약이 모두 자동 테스트된다.
- [ ] 공개 API의 동일 key·API key·path·method는 15일 동안 최초 응답을 돌려주고, 길이 초과와 처리 중 요청의 공식 오류가 테스트된다.
- [ ] 공개 API의 body-hash 충돌을 토스 호환 계약으로 주장하지 않으며, 내부 sandbox command의 동일 키·다른 본문 충돌은 별도 오류·schema로 테스트된다.
- [ ] 응답 유실 뒤 조회 또는 동일 키 재시도로 결과를 확정할 수 있다.

## 불변식과 실험

- [ ] N개 동시 부분 취소에서 성공 합계가 승인 금액을 넘지 않는다.
- [ ] Cancel 배열, balanceAmount, 원장 역분개가 일치한다.
- [ ] 커밋 전 실패와 커밋 후 응답 유실이 구분되어 테스트된다.
- [ ] 최소 두 동시성 전략을 같은 워크로드·seed로 비교했다.
- [ ] 보고서의 p95, 충돌률, 처리량에 분모·구간·원시 결과가 있다.
- [ ] 선택하지 않은 전략의 실제 관측 비용과 선택 근거를 ADR에 기록했다.

## 공통 증거 게이트

- [ ] `projects/02-payment-command-concurrency/evidence/MANIFEST.md`가 `docs/EVIDENCE_POLICY.md`의 공통 manifest 계약과 `evidenceCheck`를 통과한다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 필수 범위는 `./gradlew harnessFull`이 통과한다. 미실행이면 사유를 기록하고 상태를 `HOLD`로 둔다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
