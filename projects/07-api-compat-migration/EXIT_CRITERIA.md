# 프로젝트 07 종료 기준 — API 호환 마이그레이션

## 계약 보존

- [ ] 전환 전후 동일 contract suite가 통과한다.
- [ ] 샘플 상점은 코드 변경 없이 두 엔진에서 동작한다.
- [ ] 허용·검토·차단·미분류 semantic diff 규칙이 고정된다.
- [ ] amount, status, Cancel, 오류, 분개 차이는 차단으로 처리된다.

## 전환과 롤백

- [ ] shadow 실행이 원장·웹훅·응답 부작용을 만들지 않는다.
- [ ] backfill이 재실행 가능하고 체크섬 검증을 통과한다.
- [ ] MID canary의 진입·중단 임계치가 사전에 정의된다.
- [ ] 롤백 후 신규 엔진 결제도 조회·취소 가능하다.
- [ ] 전환·롤백 훈련 타임라인과 원시 diff가 보존된다.
- [ ] 합성 기준 구현을 실제 토스 레거시로 표현하지 않는다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
