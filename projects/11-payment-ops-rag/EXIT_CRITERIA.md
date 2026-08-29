# 프로젝트 11 종료 기준 — 결제 운영 지식 RAG

## 근거와 답변

- [ ] 모든 corpus 항목이 source registry의 URL·날짜·유형과 연결된다.
- [ ] 공식 V2 normative source와 legacy case-study가 UI와 응답에서 구분된다.
- [ ] 외부 문서 원문 전체가 저장소에 복제되지 않는다.
- [ ] 문장별 citation이 실제 근거 구간을 지지한다.
- [ ] PLANNED 문서를 구현 완료로 답하지 않는다.
- [ ] 근거 부족·상충·stale source에서 답변을 보류한다.

## 평가

- [ ] answerable/unanswerable 평가셋과 정답 근거가 있다.
- [ ] retrieval 후보를 같은 평가셋·seed로 비교했다.
- [ ] citation precision, source-type correctness, recall@k가 원시 결과와 연결된다.
- [ ] MID 자료 격리가 retrieval 단계에서 테스트된다.
- [ ] latency와 비용에 환경·분모·구간이 명시된다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
