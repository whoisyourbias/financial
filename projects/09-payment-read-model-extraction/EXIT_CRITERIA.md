# 프로젝트 09 종료 기준 — 결제 조회 모델과 서비스 경계

## projection 정합성

- [ ] event ID·aggregate version 기반 멱등 upsert가 테스트된다.
- [ ] 중복·역전·누락 사건에서 최신 상태가 보존된다.
- [ ] checkpoint 재구축 결과가 기준 체크섬과 일치한다.
- [ ] read model 장애가 승인·취소 커밋을 막지 않는다.
- [ ] schema 진화의 backward/forward 호환 테스트가 있다.

## 비교와 결정

- [ ] 동일 프로세스와 별도 프로세스를 같은 workload·seed로 비교했다.
- [ ] lag, 조회 지연, 명령 영향, 복구 시간, 비용 원시값이 있다.
- [ ] 추출 또는 유지 결론에 대안·근거·감수 비용·남은 한계가 있다.
- [ ] 선택하지 않은 검색/분석 저장소를 사용한 것처럼 기술하지 않는다.
- [ ] “MSA 전환”이나 성능 개선을 실측 없이 주장하지 않는다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
