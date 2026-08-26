# 프로젝트 11 — 금융 RAG와 모델 라우팅

| 항목           | 값                                                |
| -------------- | ------------------------------------------------- |
| 기간           | 2027-01-18 ~ 2027-01-31                           |
| 상태           | PLANNED                                           |
| 선행 프로젝트  | 05 보안, 06 운영                                  |
| 목표 태그      | `p11-financial-rag`                               |
| 주력 채용 신호 | RAG, 모델 추상화, 평가, 비용·지연·보안, 근거 제시 |

## 문제 정의

금융 운영자가 정책과 규정 문서에서 근거를 찾을 때 일반 LLM은 문서에 없는 답을 자연스럽게 만들 수 있습니다. RAG를 붙였더라도 관련 문서를 못 찾거나 잘못된 문서를 인용하면 신뢰할 수 없습니다. 이 프로젝트는 버전이 고정된 공개 금융 문서와 합성 내부 정책을 대상으로 retrieval과 최종 답변을 분리 평가하고, 상용 API와 로컬 모델을 같은 인터페이스에서 비용·지연·근거 품질·보안으로 비교합니다.

법률·금융 자문을 제공하지 않습니다. 질문 세트와 corpus 안에서 근거를 찾는 학습용 운영 도우미입니다.

## 필수 범위

- 출처·취득일·버전·checksum이 있는 document corpus
- 공개 금융 문서와 합성 내부 정책의 명확한 구분
- parsing·chunking·metadata·embedding pipeline
- document version과 access scope
- hybrid 또는 vector retrieval 후보 비교
- 답이 있는 질문, 답이 없는 질문, 적대적 질문의 고정 평가 세트
- retrieval Recall@K와 final citation correctness 분리
- 사람이 확인한 고정 표본
- 출처를 찾지 못할 때 명시적 거절
- Spring AI model abstraction
- 상용 OpenAI 호환 API와 Ollama local model
- model routing·timeout·fallback·circuit policy
- prompt·document injection과 PII masking
- model·prompt·corpus·evaluation version 기록

## 비범위

- 법률 해석의 정확성 보증
- 최신 법령 자동 동기화
- 인터넷 전체 검색
- fine-tuning·foundation model 개발
- LLM-as-a-judge 단독 평가
- 근거 없는 confidence score
- 거래·결제 변경 도구

## 평가 corpus

최소 다음 세 범주를 고정합니다.

- Answerable: corpus에 정답 근거와 문서 위치가 있는 질문
- Unanswerable: corpus만으로 답할 수 없어 거절해야 하는 질문
- Adversarial: prompt injection, document injection, 권한 밖 문서 요청, 개인정보 유도

각 질문은 question ID, expected document ID·location, 허용 핵심 사실, 거절 기대 여부를 가집니다. 생성 모델이 만든 정답을 그대로 gold로 사용하지 않습니다.

## 설계 후보

### Retrieval

- dense vector retrieval
- keyword/BM25 retrieval
- hybrid retrieval

같은 corpus·question set에서 Recall@K와 false retrieval을 비교합니다. 데이터 크기가 작아 단순 keyword가 충분하면 hybrid를 채택하지 않아도 됩니다.

### 모델 라우팅

- 모든 요청을 상용 API로 처리
- 모든 요청을 local model로 처리
- 보안·복잡도·지연 예산에 따라 route

상용 API와 local model은 동일 prompt·retrieved context에서 비교합니다. 모델 기능 차이로 prompt가 달라지면 직접 비교가 제한된다고 표시합니다.

## 계획 인터페이스

```text
POST /api/v1/knowledge/documents
POST /api/v1/knowledge/indexes/{version}/build
POST /api/v1/assistant/questions
GET  /api/v1/assistant/responses/{id}
GET  /api/v1/assistant/evaluations/{version}
```

응답은 answer, citations, refusal reason, corpus version, model ID, prompt version을 포함합니다. 모델의 내부 chain-of-thought를 저장·노출하지 않습니다.

## 데이터 흐름

```text
versioned documents
  → parse·sanitize·chunk
  → metadata + embedding/index

authenticated question
  → PII masking·scope check
  → retrieval
  → context injection filtering
  → routed model
  → answer + citations / refusal
  → evaluation·telemetry
```

## 산출물

- corpus manifest와 document license·source
- indexing pipeline과 retrieval comparison
- versioned golden question set
- Spring AI model router·API/local adapters
- citation·refusal response contract
- retrieval·answer·cost·latency evaluation
- injection·PII security tests
- RAG·model routing ADR와 limitation

## 10일 작업 계획

| 일차 | 작업                               | 완료 기준                       |
| ---- | ---------------------------------- | ------------------------------- |
| 1    | use case·corpus·평가 범주·비범위   | 자문·최신성 제한 명시           |
| 2    | retrieval·routing·평가 ADR         | gold 생성과 수동 검증 방식 고정 |
| 3    | document ingest·version·scope      | source·checksum·metadata 보존   |
| 4    | chunk·index·retrieval 후보         | same question set 결과 생성     |
| 5    | answer·citation·refusal            | 근거 없을 때 응답 억제          |
| 6    | API·Ollama adapters·routing        | timeout·fallback·version 기록   |
| 7    | PII·prompt/document injection      | 권한 밖 context 차단            |
| 8    | retrieval·수동 fact·비용·지연 평가 | 원본 response·metrics 생성      |
| 9    | model card·ADR·evidence·한계       | LLM judge 의존도 공개           |
| 10   | Portfolio Review·Red Team          | PASS와 🔴 0건 또는 HOLD         |

## 테스트와 측정

- parser 오류·빈 문서·중복 문서·버전 교체
- chunk boundary에 걸친 근거
- document scope가 다른 사용자
- answerable·unanswerable·adversarial 질문
- retrieval 후보별 Recall@K
- citation document·location 일치
- model timeout·rate limit·invalid JSON
- API 실패 뒤 local fallback과 반대 방향 정책
- prompt injection·document instruction·PII 유도
- corpus·prompt·model 변경 뒤 regression

측정 항목:

- retrieval Recall@K와 평가 question 수
- citation correctness의 수동 확인 분모
- answerable response·unanswerable refusal 수
- unsupported claim 수와 판정 규칙
- end-to-end latency p50/p95와 retrieval/model 구간
- 요청당 입력·출력 token·비용 산정 기준
- local model 장비·model size·latency

## 필요한 증거

- corpus source·취득일·checksum·license
- golden question과 expected evidence
- retrieval raw ranking
- API/local raw response와 citations
- 사람이 확인한 표본·판정자·판정 규칙
- LLM evaluator 사용 여부와 disagreement
- latency 구간·비용 계산식·model version
- injection·PII 공격 corpus와 결과

## 예상 면접 질문

1. retrieval Recall@K가 높아도 최종 답변이 틀릴 수 있는 이유는 무엇인가요?
2. LLM evaluator만 사용하지 않은 이유와 수동 평가 표본은 어떻게 정했나요?
3. 상용 API와 local model을 어떤 보안·비용·지연 기준으로 route합니까?
4. 문서 안의 prompt injection이 system instruction을 덮지 못하게 어떻게 막나요?
5. corpus에 답이 없을 때 거절하는 기준을 어떻게 평가합니까?

## 주요 위험과 다음 회차 연결

- 문서 수나 vector DB 사용을 성과로 내세우지 않습니다.
- 최신 법령을 보장하지 않고 corpus snapshot 날짜를 노출합니다.
- LLM judge 결과와 사람이 확인한 결과를 합쳐 하나의 정확도로 만들지 않습니다.
- 프로젝트 12는 이 RAG를 읽기 근거로 사용하지만 변경 실행 권한을 부여하지 않습니다.
