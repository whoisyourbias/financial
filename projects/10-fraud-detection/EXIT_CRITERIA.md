# 프로젝트 10 종료 기준 — 결제 이상거래 탐지

## 데이터와 모델

- [ ] 합성 데이터 생성 규칙, seed, feature schema가 있다.
- [ ] event time 기준 분할과 미래 정보 leakage 검사가 통과한다.
- [ ] 규칙 baseline과 모델 후보가 같은 평가셋으로 비교된다.
- [ ] decision마다 feature snapshot, reason code, model/policy version이 있다.
- [ ] 같은 입력·버전의 결과가 결정론적으로 재현된다.

## 운영과 책임

- [ ] threshold별 precision, recall, review volume, 오탐 비용이 함께 제시된다.
- [ ] drift와 입력 품질 저하가 탐지된다.
- [ ] 사람 override와 rollback이 감사된다.
- [ ] 실제 결제 차단은 별도 gate 없이는 연결되지 않는다.
- [ ] 합성 데이터 결과를 실제 고객 효과로 일반화하지 않는다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
