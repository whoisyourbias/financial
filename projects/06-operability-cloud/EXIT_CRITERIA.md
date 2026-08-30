# 프로젝트 06 종료 기준 — 운영성과 클라우드

## 관측과 복구

- [ ] `SLI-CARD-SUCCESS`, `SLI-CARD-P95`, `SLI-VA-SUCCESS`, `SLI-VA-P95`, `SLI-WEBHOOK-AGE`, `SLI-RECON-OPEN`의 분모·구간·단위가 정의된다.
- [ ] 두 샘플 상점 여정의 end-to-end trace가 결제·원장·웹훅 또는 가상계좌·대사를 연결한다.
- [ ] 대시보드 수치와 원시 metric query 결과가 일치한다.
- [ ] 사전 정의한 두 장애에서 경보·진단·복구 타임라인이 남는다.
- [ ] readiness, liveness와 graceful shutdown이 실제로 검증된다.
- [ ] 로그·trace·metric label에 secret/PII가 없다.

## 배포 쇼케이스

- [ ] IaC로 같은 비프로덕션 환경을 재생성할 수 있다.
- [ ] candidate 배포와 사전 기록한 baseline container digest·backward-compatible DB schema로의 수동 롤백 결과가 기록된다.
- [ ] 비용 상한·리소스 목록·삭제 절차가 있다.
- [ ] manifest의 `intendedTags`에 `p06-operability-cloud`, `showcase-01-payment-sandbox`가 준비된다. 실제 tag 생성·검증은 review 뒤 공통 `RELEASED` 게이트에서 수행한다.
- [ ] “고가용성/대용량/무중단” 문구에는 검증 범위가 붙는다.

## 공통 증거 게이트

- [ ] `projects/06-operability-cloud/evidence/MANIFEST.md`가 `docs/EVIDENCE_POLICY.md`의 공통 manifest 계약과 `evidenceCheck`를 통과한다.
- [ ] 실행 명령, Git SHA, 환경, 데이터 seed, 원시 결과가 연결되어 있다.
- [ ] `./gradlew harnessFast`와 `./gradlew knowledgeCheck`가 통과한다.
- [ ] Docker가 필요한 필수 범위는 `./gradlew harnessFull`이 통과한다. 미실행이면 사유를 기록하고 상태를 `HOLD`로 둔다.
- [ ] 구현·재현하지 않은 결과를 `[검증됨]`이나 성과로 표현하지 않는다.
- [ ] 실제 고객 데이터·자격증명·개인정보가 source, fixture, log, manifest에 없다.
