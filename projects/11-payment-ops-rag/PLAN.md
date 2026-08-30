# 프로젝트 11 — 결제 운영 지식 RAG

## 목표

등록된 토스페이먼츠 V2 공개 근거와 저장소의 실행 가능한 런북을 분리 인용해, 결제 운영 질문에 출처·버전·근거 유형을 포함한 답을 생성합니다.

## 필수 범위

- 공식 문서 URL과 짧은 파생 사실
- 저장소 정책, 계약 매트릭스, 런북, OpenAPI
- source registry와 chunk provenance
- retrieval baseline 1개와 challenger 1개 비교
- 답변별 인용, 날짜와 네 가지 근거 권위 표시
- 근거 부족 시 답변 보류
- 합성 장애 질문 평가셋

외부 문서 원문 전체를 저장소에 복제하지 않습니다. 라이선스와 접근 정책이 허용된 짧은 파생 사실마다 source URL, `checkedAt`, heading/field anchor, 정규화한 원문 fragment의 `sourceFragmentHash`, 파생 사실의 `derivedFactHash`와 `supersedes` 관계를 저장합니다. refresh에서 anchor가 사라지거나 `sourceFragmentHash`가 바뀌면 stale/contract diff로 판정합니다. 평가 시 이 레코드를 immutable corpus version으로 묶어 원문 변경 뒤에도 어떤 사실을 사용했는지 재현합니다.

## 근거 권위와 상태

| 권위 | 예 | 사용 규칙 |
| --- | --- | --- |
| `normative` | 토스페이먼츠 V2 공식 문서의 등록된 파생 사실 | 공개 계약 답변의 최우선 근거 |
| `case-study` | 레거시 기술 아티클 | 문제 사례에만 사용, 현재 계약·구현 증거로 금지 |
| `project-policy` | 저장소 정책·계약 매트릭스·런북 | 이 샌드박스의 내부 결정 설명 |
| `implementation-evidence` | OpenAPI, test·manifest·결과 | 현재 구현·검증 상태 설명 |

- chunk 상태는 문서 기본값보다 section·claim의 `[가정]`·`[검증됨]`·`[불명]`·`[모순]` 라벨을 우선합니다.
- 현재 사실과 계획이 섞인 section은 claim 단위로 분리하지 못하면 ingest를 실패시킵니다. 라벨 없는 claim의 기본값은 `planned`입니다.
- `deprecated`·`excluded` source는 검색 index에서 제거하고 superseding 문서 ID만 registry에 남깁니다.
- 프로젝트 11 구현 전에 기존 catalog의 혼합 상태 문서와 이전 리뷰를 이 규칙으로 migration하고 golden test로 고정합니다.

## 확장 범위

- 추가 reranker·모델 router와 세 번째 retrieval 후보
- 브라우저 운영 UI와 online latency dashboard

source registry, baseline·challenger, 문장 인용, 보류 정책과 고정 평가셋이 필수이며 확장은 release를 막지 않습니다.

## 불변식

1. 모든 답변 문장은 최소 하나의 추적 가능한 근거와 연결됩니다.
2. normative·case-study·project-policy·implementation-evidence를 같은 권위로 섞지 않습니다.
3. claim·section 상태가 `PLANNED`이면 구현 완료처럼 답하지 않고, 혼합 chunk는 ingest하지 않습니다.
4. 날짜·버전이 충돌하면 최신 normative source를 우선하고 충돌을 표시합니다.
5. MID별 운영 자료는 검색 단계부터 격리합니다.
6. retrieval 실패나 상충 근거에서는 추측 대신 보류합니다.

## 질의 예시

- “승인 타임아웃 뒤 무엇을 확인해야 하나?”
- “웹훅 재전송 간격과 최종 실패 처리 절차는?”
- “가상계좌 입금은 완료됐는데 콜백이 안 왔다.”
- “부분 취소 후 정산 adjustment가 왜 생겼나?”
- “이 오류 코드가 공개 계약인지 샌드박스 정책인지?”

## 구현 순서

1. source registry의 권위·claim 상태·anchor·`sourceFragmentHash`·`derivedFactHash`·supersedes 스키마를 고정합니다.
2. corpus ingest에서 원문 저장 금지, 혼합 chunk 실패, deprecated 제외와 checksum을 강제합니다.
3. 선택한 retrieval baseline과 challenger를 같은 평가셋으로 비교합니다.
4. 문장 단위 citation과 근거 유형 표시를 구현합니다.
5. stale/conflict/insufficient-evidence 테스트를 추가합니다.
6. 고정 corpus·질문셋으로 retrieval·citation·보류 품질을 재현합니다.

## 평가

- answerable/unanswerable 정확도
- citation precision과 source-type correctness
- stale source 탐지율
- retrieval recall@k와 rerank 변화
- p50/p95 latency와 비용
- 잘못된 구현 완료 주장 건수

## 검증 산출물

- 평가 질문·정답 근거·채점 스크립트
- retrieval ablation 보고서
- citation 및 보류 정책 테스트
- source freshness manifest
- `projects/11-payment-ops-rag/evidence/`

## 근거

- `docs/TOSS_PAYMENTS_EVIDENCE_MAP.md`
- source refs: `toss-payments-llms-index`, `toss-payments-llm-quick-reference`와 각 주제별 V2 문서
- 레거시 시리즈는 case-study 라벨이 있을 때만 검색 결과에 노출합니다.
