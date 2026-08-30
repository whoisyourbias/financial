# 프로젝트 01 종료 기준 — 결제 계약과 불변 결제 원장

## 계약과 기능

- [ ] `TP-PAY-001..003`, `TP-WIRE-001`, `TP-WIRE-003`, `TP-WIRE-005..006`, `TP-STATE-001`, `SBX-001`의 적용 여부가 매트릭스에 기록되고 `TP-WIRE-002`, `INT-AUTH-001`, `TP-WIRE-004`의 후속 소유 프로젝트가 명시된다.
- [ ] 세션 생성 → 승인 → paymentKey/orderId 조회가 샘플 상점에서 동작한다.
- [ ] 공개 경로와 `/sandbox/**` 경로가 명확히 분리된다.
- [ ] 합성 시크릿의 `Basic base64(secret:)` 정상·누락·scheme/Base64 오류·콜론 누락·잘못된 키가 테스트되고, MID/환경 격리·수명주기·역할은 `INT-AUTH-001`과 프로젝트 05 범위로 표시된다.
- [ ] 지원하지 않는 필드·결제수단은 호환으로 표시하지 않는다.

## 불변식과 테스트

- [ ] paymentKey/orderId/amount 불일치가 계약상 오류로 거절된다.
- [ ] 같은 `paymentKey`의 중복 승인이 새 Payment·journal을 만들지 않으며, 이를 공개 `Idempotency-Key` 응답 재생으로 표현하지 않는다.
- [ ] 승인과 대응 분개가 원자적으로 커밋·롤백된다.
- [ ] 모든 분개에서 차변 합계와 대변 합계가 같다.
- [ ] 게시 분개는 수정·삭제가 아니라 역분개로만 보정된다.
- [ ] 금액 계산에 `double`과 `float`가 없다.
- [ ] OpenAPI와 golden response 스냅샷이 회귀 테스트에 포함된다.

## 설명 가능성

- [ ] 공개 응답 모델과 내부 원장 모델의 경계를 설명하는 ADR이 있다.
- [ ] 선택한 금액 타입과 잔액 전략의 대안·비용·한계가 실제 실험 범위에서 기록된다.

## 공통 증거 게이트

- [ ] `projects/01-ledger-core/evidence/MANIFEST.md`가 `docs/EVIDENCE_POLICY.md`의 공통 manifest 계약과 `evidenceCheck`를 통과한다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 필수 범위는 `./gradlew harnessFull`이 통과한다. 미실행이면 사유를 기록하고 상태를 `HOLD`로 둔다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
