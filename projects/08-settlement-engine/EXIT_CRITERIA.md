# 프로젝트 08 종료 기준 — 상점 정산 엔진

## 계산과 원장

- [ ] gross, cancel, fee, tax, net 산식과 반올림 truth table이 있다.
- [ ] 모든 정산 라인에 수수료 정책 version snapshot이 있다.
- [ ] 정책 변경 전후 결제가 각 시점 정책으로 계산된다.
- [ ] 확정 정산은 수정되지 않고 adjustment로 보정된다.
- [ ] 산술 관계와 금액 보존 속성 테스트가 통과한다.

## 배치와 경계

- [ ] 주말·공휴일·월말 settlement date가 테스트된다.
- [ ] 부분 실패 후 restart가 라인을 중복 확정하지 않는다.
- [ ] 정산 전·후 취소와 음수 이월 경계가 검증된다.
- [ ] 운영 API는 `/sandbox/ops/**`이며 공개 호환으로 표시하지 않는다.
- [ ] 실제 상점 수수료나 토스 내부 산식 재현 주장이 없다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
