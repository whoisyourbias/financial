# 프로젝트 06 종료 기준 — 운영성과 클라우드

## 관측과 복구

- [ ] 승인·취소·웹훅·대사 SLI의 분모·구간·단위가 정의된다.
- [ ] 샘플 상점 end-to-end trace가 결제·원장·웹훅을 연결한다.
- [ ] 대시보드 수치와 원시 metric query 결과가 일치한다.
- [ ] 장애 주입에서 경보·진단·복구 타임라인이 남는다.
- [ ] readiness, graceful shutdown, 백업 복구가 실제로 검증된다.
- [ ] 로그·trace·metric label에 secret/PII가 없다.

## 배포 쇼케이스

- [ ] IaC로 같은 비프로덕션 환경을 재생성할 수 있다.
- [ ] 배포와 롤백 결과가 기록된다.
- [ ] 비용 상한·리소스 목록·삭제 절차가 있다.
- [ ] `showcase-01-payment-sandbox` 태그 manifest가 있다.
- [ ] “고가용성/대용량/무중단” 문구에는 검증 범위가 붙는다.

## 공통 증거 게이트

- [ ] `evidence/manifest.schema.json`을 통과하는 manifest가 있다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 범위는 `./gradlew harnessFull` 결과 또는 미실행 사유가 있다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
