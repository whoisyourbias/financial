# 프로젝트 11 — 결제 운영 지식 RAG

## 목표

등록된 토스페이먼츠 V2 공개 근거와 저장소의 실행 가능한 런북을 분리 인용해, 결제 운영 질문에 출처·버전·근거 유형을 포함한 답을 생성합니다.

## 범위

- 공식 문서 URL과 짧은 파생 사실
- 저장소 정책, 계약 매트릭스, 런북, OpenAPI
- source registry와 chunk provenance
- hybrid retrieval와 reranking 후보 비교
- 답변별 인용, 날짜, normative/case-study 표시
- 근거 부족 시 답변 보류
- 합성 장애 질문 평가셋

외부 문서 원문 전체를 저장소에 복제하지 않습니다. 라이선스와 접근 정책이 허용된 짧은 파생 사실·해시·URL을 저장합니다.

## 불변식

1. 모든 답변 문장은 최소 하나의 추적 가능한 근거와 연결됩니다.
2. 공식 V2 계약과 레거시 개선 사례를 같은 권위로 섞지 않습니다.
3. 저장소 구현 상태가 `PLANNED`이면 구현 완료처럼 답하지 않습니다.
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

1. source registry와 파생 사실 스키마를 고정합니다.
2. corpus ingest에서 원문 저장 금지와 checksum을 강제합니다.
3. keyword/vector/hybrid 후보를 같은 평가셋으로 비교합니다.
4. 문장 단위 citation과 근거 유형 표시를 구현합니다.
5. stale/conflict/insufficient-evidence 테스트를 추가합니다.
6. 운영 UI와 latency·retrieval 품질 지표를 연결합니다.

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
- `evidence/projects/11/`

## 근거

- `docs/TOSS_PAYMENTS_EVIDENCE_MAP.md`
- source refs: `toss-payments-llms-index`, `toss-payments-llm-quick-reference`와 각 주제별 V2 문서
- 레거시 시리즈는 case-study 라벨이 있을 때만 검색 결과에 노출합니다.
