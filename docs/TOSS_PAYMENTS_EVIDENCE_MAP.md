# 토스페이먼츠 근거 지도

## 목적과 판정 규칙

이 문서는 토스페이먼츠 공개 자료를 프로젝트 요구사항으로 번역하는 출처 지도입니다. 원문은 저장소에 복제하지 않고 URL과 확인일을 `docs/domain/sources.yaml`에서 관리합니다.

- **규범 근거**: V2 개발자 문서의 외부 계약입니다. 선정 API의 경로·필드·상태·오류·헤더와 웹훅 정책을 결정합니다.
- **사례 근거**: Toss Tech의 공개된 개선 경험입니다. 설계 후보와 실패 시나리오를 찾는 데 사용하지만 토스 내부 구현을 복제하거나 프로젝트 성과로 주장하지 않습니다.
- **프로젝트 결정**: 공개 계약을 만족하기 위해 이 저장소가 선택한 구조입니다. 구현·실험 전에는 `[가정]`입니다.
- **미확인**: 공개 자료로 알 수 없는 실제 카드망 전문, 내부 DB schema, 조직·인프라 상세입니다.

## 선정 제품 경계

가맹점이 호출하는 학습용 PG 샌드박스를 구현합니다. 공통 KRW 카드·간편결제와 가상계좌 시나리오에서 승인·조회·전액/부분 취소·웹훅을 호환 대상으로 삼습니다.

빌링, 브랜드페이, 링크페이, 지급대행, 해외결제, 에스크로, 실제 세금 신고는 비범위입니다. 비범위 필드는 조용히 무시하지 않고 계약 매트릭스에 `unsupported`로 남깁니다.

## 규범 근거 묶음

| 근거 묶음 | sourceRefs | 외부에서 관찰한 것 | 적용 |
| --- | --- | --- | --- |
| 탐색·요약 | `toss-payments-llms-index`, `toss-payments-llm-quick-reference` | V2 문서 구조, 결제 흐름과 자주 틀리는 연동 규칙 | 전체 문서 수집과 변경 감지 |
| 결제 흐름 | `toss-payments-online-payment`, `toss-payments-payment-flow`, `toss-payments-transaction-flow` | 요청·인증·승인, Payment와 거래 식별자의 역할 | 01·02·07 |
| 승인·조회·취소 | `toss-payments-core-api`, `toss-payments-cancel-guide`, `toss-payments-error-codes` | 선정 endpoint, Payment·Cancel·Error 응답, 부분 취소 | 01·02 |
| 재시도·계약 | `toss-payments-authorization`, `toss-payments-idempotency`, `toss-payments-request-response`, `toss-payments-versioning` | Basic 인증, 멱등키, JSON·HTTP 응답, 버전 경계 | 02·05·07 |
| 가상계좌 | `toss-payments-virtual-account`, `toss-payments-virtual-account-webhook`, `toss-payments-payment-methods` | 발급과 입금 완료의 분리, 입금 전후 취소 차이 | 04 |
| 웹훅 | `toss-payments-webhook-guide`, `toss-payments-webhook-events` | MID별 등록, event payload, 10초 내 200, 최대 7회 재전송 | 03·04 |
| 보안·운영 | `toss-payments-api-keys`, `toss-payments-security`, `toss-payments-secret-key-practice`, `toss-payments-status-page`, `toss-payments-deploy-checklist` | 키 경계·회전, TLS, 장애 인지와 배포 전 검증 | 05·06 |
| 타입·정산 | `toss-payments-data-types`, `toss-payments-enum-codes`, `toss-payments-settlement` | wire type·enum, 상점 계약에 따른 정산 금액·지급일 | 01·08 |

## 레거시 개선 사례 지도

| 공개 사례 | 프로젝트에 가져올 질문 | 적용 |
| --- | --- | --- |
| `toss-legacy-payment-window`, `toss-legacy-sdk` | 가맹점 계약을 바꾸지 않고 내부를 어떻게 교체하며 다양한 실행 환경을 어떻게 관측할 것인가? | 06·07 |
| `toss-legacy-open-api` | OAS와 서버·문서를 어떻게 동기화하고 인증·오류·버전 규약을 공통화할 것인가? | 02·05·07 |
| `toss-legacy-ledger` | 외부 승인 timeout, 상위/하위 timeout 불일치, 이벤트 누락·중복을 어떻게 복구할 것인가? | 01·02·03·06 |
| `toss-legacy-settlement` | 거래 단위 계산, 계약 snapshot, 실패 건 재처리를 어떻게 증명할 것인가? | 08 |
| `toss-legacy-data-serving` | 원장을 보호하면서 검색·집계용 read model을 어떻게 만들고 보정할 것인가? | 09 |
| `toss-legacy-configuration`, `toss-legacy-infrastructure` | 설정 중복과 환경 차이를 어떻게 테스트하고 배포·복구 비용을 기록할 것인가? | 06·09 |
| `toss-legacy-zero-trust`, `toss-legacy-pqc-ko`, `toss-legacy-pqc-en` | 오래된 가맹점 호환성을 깨지 않고 보안 수준을 어떻게 단계적으로 높일 것인가? | 05·07 |

`toss-legacy-series`, `toss-legacy-intro`, `toss-legacy-overview`는 시리즈의 전체 맥락을 확인하는 인덱스입니다. 영문 PQC 글은 한국어 글의 별도 독립 근거가 아니라 공식 번역본으로 취급합니다.

## 프로젝트 공통 사용 절차

1. 구현 직전 해당 source URL과 `checkedAt`을 다시 확인합니다.
2. 외부 관찰을 `DOMAIN_BRIEF.md`에 기록합니다.
3. 관찰을 `TOSS_PAYMENTS_CONTRACT_MATRIX.md`의 테스트 가능한 행으로 번역합니다.
4. 문서에 없는 내부 선택은 ADR에서 `[가정]`으로 분리합니다.
5. 코드·계약 테스트·원본 evidence가 연결된 범위만 `[검증됨]`으로 승격합니다.

## 미확인과 금지 주장

- 실제 토스페이먼츠의 table, transaction boundary, 원장 계정과목, 파트너 전문은 미확인입니다.
- 기술 아티클의 수치와 구조는 토스의 공개 사례이며 이 프로젝트의 규모·성능·운영 경험이 아닙니다.
- 실제 카드번호, 계좌번호, API key, 고객정보를 fixture·log·evidence에 넣지 않습니다.
- `토스페이먼츠 호환`은 아래 매트릭스에서 구현·통과한 선정 행에만 사용합니다.
