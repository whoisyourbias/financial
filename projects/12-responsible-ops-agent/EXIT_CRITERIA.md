# 프로젝트 12 종료 기준 — 책임 있는 결제 운영 에이전트

## 정책 경계

- [ ] 모든 도구에 risk class, Agent 동작, 실제 호출 주체, credential owner, 승인자, executor와 MID scope가 선언된다.
- [ ] 모델 출력만으로 도구 권한이나 정책이 바뀌지 않는다.
- [ ] 금융 쓰기는 명시적 승인 없이는 실행되지 않는다.
- [ ] 승인이 대상·금액·요청 해시·만료와 묶인다.
- [ ] 승인 후 상태 변경에서 재검증이 실행을 차단한다.
- [ ] 쓰기 도구 재시도가 같은 부작용을 중복 만들지 않는다.
- [ ] Agent process가 APPROVE·EXECUTE credential을 갖지 않고 별도 사람 approval API와 deterministic executor만 write를 수행한다.

## 공격과 복구

- [ ] prompt injection, 권한 상승, cross-MID, replay 테스트가 있다.
- [ ] 정책 위반·승인 없는 금융 쓰기 0건이 원시 로그로 검증된다.
- [ ] kill switch가 신규·queued·pre-commit 쓰기를 차단하고, commit 이후 효과는 감사·대사·명시적 보상으로 처리한다.
- [ ] executor transaction commit의 linearization point와 model/policy rollback·금융 보상의 차이가 테스트된다.
- [ ] 실패 후 사람이 수행할 수동 런북이 있다.
- [ ] 모델·도구·승인·실행의 감사 사슬이 하나의 trace로 연결된다.

## 공통 증거 게이트

- [ ] `projects/12-responsible-ops-agent/evidence/MANIFEST.md`가 `docs/EVIDENCE_POLICY.md`의 공통 manifest 계약과 `evidenceCheck`를 통과한다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 필수 범위는 `./gradlew harnessFull`이 통과한다. 미실행이면 사유를 기록하고 상태를 `HOLD`로 둔다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
