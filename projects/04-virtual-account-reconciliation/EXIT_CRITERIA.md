# 프로젝트 04 종료 기준 — 가상계좌 입금과 대사

## 계약과 상태

- [ ] 가상계좌 발급 선택 계약과 `DEPOSIT_CALLBACK` 계약 테스트가 있다.
- [ ] 샌드박스 입금 API가 공개 경로와 분리된다.
- [ ] 정확한 입금에서만 `WAITING_FOR_DEPOSIT → DONE`이 된다.
- [ ] 같은 입금 참조번호가 분개·웹훅을 중복 생성하지 않는다.
- [ ] 입금 전 취소와 입금 후 취소 규칙이 각각 테스트된다.

## 대사와 복구

- [ ] 다섯 대사 분류가 fixture로 검증된다.
- [ ] batch 부분 실패 후 restart가 완료 chunk를 중복 반영하지 않는다.
- [ ] 자동 보정과 사람 검토 기준이 문서화된다.
- [ ] 보정은 원본을 수정하지 않고 감사 가능한 기록을 추가한다.
- [ ] 실제 계좌·입금자 개인정보가 fixture와 log에 없다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
