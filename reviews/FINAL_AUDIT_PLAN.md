# 최종 2주 통합 감사 계획

## 기간과 목적

- 기간: 2027-02-15 ~ 2027-02-28
- 입력: 프로젝트 01~12의 코드, 결과, evidence, 대시보드, 다이어그램, 리뷰
- 목적: 개별 회차의 PASS를 다시 검증하고 채용 담당자가 읽을 네 대표 사례로 재구성
- 원칙: 이 기간에는 새 핵심 기능을 추가하지 않습니다.

## Day 1 — 전수 인벤토리

- 12개 프로젝트의 status, tag, 결과 문서, evidence manifest를 열거합니다.
- 누락·읽기 실패·미확인 이미지를 별도로 기록합니다.
- 계획 스택과 실제 사용 스택을 비교해 미사용 기술을 제거합니다.
- HOLD 프로젝트가 있으면 대표 사례 포함을 중단합니다.
- 공식 source registry를 refresh하고 checkedAt·heading/field anchor·content hash의 contract diff를 기록합니다.

## Day 2 — 금융 불변식 회귀

- 승인·조회·취소, 원장, 웹훅, 가상계좌, 정산 전체 회귀 테스트를 같은 commit에서 실행합니다.
- 시작·종료 총액, 거래별 차변·대변, 중복 키, 상태 전이를 다시 확인합니다.
- 프로젝트별로 달랐던 fixture와 통화·반올림 정책을 대조합니다.

## Day 3 — 이벤트·배치·외부 장애

- broker 중단, duplicate·out-of-order, consumer restart를 다시 주입합니다.
- batch 중단·재시작과 상점 webhook 재전송을 통합 시나리오로 실행합니다.
- 이전 회차와 측정 조건이 달라졌다면 과거 숫자를 직접 비교하지 않습니다.

## Day 4 — 보안·권한·비밀정보

- 역할별 API 접근, audit 누락, 로그 개인정보, repository secret을 검사합니다.
- AI proposal과 approval·execute 경계를 우회하는 요청을 재실행합니다.
- 오래된 승인, 중복 승인, proposal 변경 뒤 실행을 검증합니다.

## Day 5 — AI 회귀평가

- 고정된 FDS test set으로 규칙과 모델을 다시 실행합니다.
- RAG corpus·질문 세트·prompt·model 버전을 고정하고 retrieval과 답변 평가를 분리합니다.
- prompt·document·tool injection corpus를 실행합니다.
- AI 모델 버전이 바뀌었다면 이전 성과표를 새 결과로 교체합니다.

## Day 6 — 2차 클라우드 쇼케이스

- Terraform으로 AWS ECS/RDS 임시 환경을 배포합니다.
- 합성 상점 시나리오로 결제·가상계좌, FDS 검토, RAG, proposal·approval을 시연합니다.
- 부하 조건, 오류율, p50/p95/p99, AI 비용·지연을 새로 측정합니다.
- 화면과 dashboard에는 동일한 시간 범위를 사용합니다.

## Day 7 — 장애·복구 시연과 정리

- 사전에 정한 세 가지 장애를 주입하고 탐지·완화·복구 시간을 기록합니다.
- 실제 on-call이 아니라 계획된 chaos experiment라고 명시합니다.
- evidence를 수집한 뒤 AWS 리소스를 제거하고 Terraform plan과 비용을 확인합니다.

## Day 8 — 채용 패키지 후보와 1차 전수 평가

`hiring-sim-portfolio-review`를 전체 저장소에 적용합니다.

- 네 대표 사례 각각의 기술적 의사결정, 문제 해결, 성과·임팩트를 평가합니다.
- 모든 대표 숫자를 raw evidence와 대조합니다.
- 루트 README, 네 사례 요약, 진화 timeline, 면접 질문·evidence 링크를 먼저 candidate 형태로 갱신하고 결과표·dashboard·다이어그램과 상호 대조합니다.
- 지원할 실제 공고 3~5개와 대표 사례를 연결합니다.
- 한 영역이라도 부적합인 사례는 대표 사례에서 제외하거나 보완합니다.
- Portfolio Review와 Red Team을 모두 1차 실행하고 지적을 계약·계획·증거에 보정합니다.
- `reviews/evidence/MANIFEST.md`를 `projectId: final-audit`, `intendedTags: [showcase-02-ai-payment-ops]`로 생성하고 12개 프로젝트 manifest checksum과 통합 산출물을 연결합니다.
- 보정이 루트 README·reviews·결과·evidence allowlist 밖의 계약·계획·코드·설정·migration·OpenAPI를 바꾸면 기존 evidence를 무효화합니다. 새 변경을 `sourceSha`로 삼아 Day 2~7의 영향받은 회귀·측정·cloud 제거와 manifest 생성을 다시 통과한 뒤 Day 8을 처음부터 반복합니다.
- allowlist 안의 패키징만 남고 aggregate `evidenceCheck`가 통과한 공개 payload를 commit `C0`으로 동결합니다.

## Day 9 — 동결 candidate 재심사

`hiring-sim-portfolio-redteam`을 전체 저장소와 모든 이미지에 적용합니다.

- L1: 모든 수치·단위·분모·백분위·피크 대조
- L2: 서사·코드·다이어그램·스택 대조
- L3: 네 대표 주장에 대한 최악의 후속 질문 생성
- 8종 감점 유형과 기술 결정 5요소를 전수 검사
- 🔴가 하나라도 남으면 공개 패키지를 완료하지 않습니다.
- 두 스킬을 동결 payload `C0`에 실행하고 각 결과 front matter의 `reviewedSha`를 `C0`로 기록합니다.
- `reviews/FINAL_PORTFOLIO_REVIEW.md`, `reviews/FINAL_REDTEAM_REVIEW.md` 두 파일만 추가한 직계 후속 attestation commit `C1`을 만듭니다. `C0..C1`에 다른 tracked 변경이 있으면 실패입니다.
- Portfolio Review PASS와 🔴 High 0이 아니면 tag하지 않습니다. 보정 뒤 새 payload `C0`를 만들고 두 심사를 모두 다시 실행하며, allowlist 밖 변경이면 Day 2~8로 돌아갑니다.

## Day 10 — 무변경 release 검증과 tag

- attestation commit `C1` 이후 tracked file 변경이 0건인지 확인합니다.
- aggregate manifest의 `sourceSha..C0` allowlist, 12개 하위 checksum, artifact checksum, cloud 제거와 `intendedTags`를 검증하고 두 review report가 같은 `reviewedSha=C0`를 가리키는지 확인합니다.
- 문서 수정이 필요하면 tag하지 않고 새 payload를 동결해 Day 9 재심사로 돌아갑니다. allowlist 밖 변경이면 Day 2~8을 다시 수행합니다.
- 최종 tag `showcase-02-ai-payment-ops`를 attestation commit `C1`에 생성하고 tag 검증 후 `RELEASED`로 승격합니다.

## 최종 산출물

- 채용 담당자용 루트 README
- 네 대표 사례 요약
- 12개 프로젝트 timeline
- 최종 아키텍처 다이어그램
- 통합 성능·정합성·AI 평가 결과
- Portfolio Review 결과
- Red Team 결과
- 면접 질문·답변 근거집
- AWS 제거·비용 확인 기록
- `reviews/evidence/MANIFEST.md` 통합 manifest

## 완료 조건

- 대표 사례 네 개 모두 Portfolio Review PASS
- 🔴 High 0건
- 공개된 모든 숫자가 raw evidence로 역추적 가능
- 실제 사용·운영·규제 인증으로 오해될 문구 0건
- 모든 이미지와 캡션의 값·단위·시간 범위 일치
- 미사용 기술과 빈 결과 섹션 제거
- 최종 cloud 리소스 제거 확인
