# 도메인 지식 학습 규칙

## 목적

금융 용어를 암기하는 대신, 공개된 제품 행위를 금액·상태·실패·운영 규칙으로 번역하고 코드와 테스트로 검증합니다. 대표 제품과 모방 범위는 [제품 비전](PRODUCT_VISION.md)을 따릅니다.

## 출처 우선순위

1. 토스페이먼츠 개발자센터의 `llms.txt`, V2 API reference, integration guide, glossary
2. 금융위원회, 금융감독원, 한국은행, 금융결제원, 국가법령정보센터 같은 공식 기관 자료
3. 회계·결제 네트워크의 공식 표준과 공급자 문서
4. 토스 기술 블로그와 결제 레거시 개선 시리즈는 문제·대안·운영 질문을 찾는 사례 연구로만 사용
5. 발표와 채용공고는 문제 발견과 추가 검색에만 사용

변경될 수 있는 외부 자료는 적용하는 날 다시 확인합니다. 검색 결과 요약이나 모델 기억만으로 금융 규칙을 확정하지 않습니다. 원문을 저장소에 복제하지 않고 `docs/domain/sources.yaml`에 URL, 발행 기관, 확인일과 적용 범위만 기록합니다.

## 세 상태를 분리하기

| 구분 | 의미 | 예시 |
| --- | --- | --- |
| 외부 관찰 | 공식 공개 문서에서 확인한 제품 행위 | Payment 객체가 `PARTIAL_CANCELED` 상태를 가질 수 있음 |
| 프로젝트 결정 | 공개 행위를 만족시키기 위해 우리가 선택한 설계 | 취소 누계를 원장 reversal posting과 연결 |
| 미확인 | 공개 문서로 알 수 없거나 현재 범위 밖인 내용 | 토스페이먼츠 내부 원장의 table과 transaction boundary |

외부 관찰과 프로젝트 결정을 같은 문장으로 섞지 않습니다. 구현·재현 전 프로젝트 결정은 `[가정]`이며 테스트와 evidence를 통과한 범위만 `[검증됨]`으로 승격합니다.

## 계약으로 번역하는 순서

1. `docs/domain/sources.yaml`에서 공식 URL·확인일·적용 범위를 등록합니다.
2. `docs/TOSS_PAYMENTS_EVIDENCE_MAP.md`에서 normative source와 case-study를 분리합니다.
3. `docs/TOSS_PAYMENTS_CONTRACT_MATRIX.md`에 경로, 필드, 상태, 오류, 검증 사례를 계약 ID로 기록합니다.
4. public contract와 샌드박스 전용 `/sandbox/**` API를 분리합니다.
5. golden fixture와 contract test가 생기기 전에는 구현 완료나 호환으로 표시하지 않습니다.
6. 문서가 바뀌면 checkedAt, 계약 fixture, 영향 프로젝트를 함께 재검토합니다.

## 프로젝트별 산출물

각 프로젝트는 구현 전에 같은 디렉터리에 `DOMAIN_BRIEF.md`를 만들고 다음 항목을 채웁니다.

```text
# 도메인 브리프

## 해결할 사용자 문제
## 공식 문서에서 관찰한 행위
## 프로젝트가 내린 결정
## 액터와 책임
## 용어와 식별자
## 상태 전이
## 금액 흐름과 원장 예제
## 불변식
## 실패·재시도·복구 표
## 출처
## 미확인과 비범위
## 설명할 수 있어야 하는 질문
```

문서가 구현보다 먼저 존재해야 하지만, 구현 결과인 것처럼 쓰지 않습니다. 프로젝트가 끝나면 실제 코드·테스트와 대조해 유지하거나 수정합니다.

## 도메인 카드 질문

새 command, event 또는 batch를 추가할 때 다음 질문에 답합니다.

1. 요청 주체와 돈의 현재 소유자·채권자·채무자는 누구인가?
2. 금액은 어느 시점에 예약, 승인, 확정, 취소 또는 정산되는가?
3. `pending`, `available`, `completed`는 각각 어떤 사실을 뜻하는가?
4. 원본 기록을 수정하지 않고 어떻게 반대 거래로 정정하는가?
5. timeout 뒤 성공 여부를 모르면 어느 식별자로 조회하고 확정하는가?
6. 같은 요청이나 event가 반복되면 같은 결과를 어떻게 보장하는가?
7. event가 늦거나 순서가 바뀌면 현재 상태를 어떻게 복구하는가?
8. 내부 원장과 외부 명세가 다르면 어느 쪽도 자동으로 진실이라고 가정하지 않고 어떻게 조사하는가?
9. 사람의 승인이나 역할 분리가 필요한 변경은 무엇인가?
10. 이 답의 공식 출처와 아직 확인하지 못한 부분은 무엇인가?

## 2주 학습 리듬

| 시점 | 도메인 작업 | 완료 조건 |
| --- | --- | --- |
| 1일차 | 공식 문서에서 관련 흐름과 용어 수집 | `sourceRefs`, 용어, 미확인 목록 기록 |
| 1일차 | 하나의 정상 시나리오를 금액으로 설명 | 시작·종료 금액과 액터가 명확함 |
| 2일차 | 상태 머신, 원장 예제, 실패 표 작성 | 각 행을 테스트로 바꿀 수 있음 |
| 3~7일차 | 구현 중 발견한 가정을 브리프에 환류 | 코드에만 숨어 있는 금융 규칙이 없음 |
| 8일차 | 중복·timeout·순서 역전·부분 실패 실험 | 예상 결과와 원본 증거가 연결됨 |
| 9일차 | 공식 관찰, 프로젝트 결정, 한계 대조 | 과장하거나 내부 구현을 추정한 표현이 없음 |
| 10일차 | 구두 설명과 리뷰 | 면접 질문에 숫자 예제로 답할 수 있음 |

## 테스트로 번역하는 법

| 도메인 문장 | 테스트 형태 |
| --- | --- |
| 같은 주문을 두 번 승인하면 안 된다 | 같은 멱등키·같은 본문, 같은 키·다른 본문, 동시 요청 |
| 부분 취소 합계는 승인 금액을 넘지 않는다 | 경계값, 동시 취소, 재시도, overflow |
| 외부 성공과 내부 실패가 동시에 남으면 안 된다 | transaction rollback, timeout 후 status query, 대사 차이 |
| webhook은 중복될 수 있다 | 같은 event ID 재전송과 고유 처리 수 확인 |
| 정산 합계만 맞아도 개별 거래가 틀릴 수 있다 | 합계 대사와 식별자별 대사를 별도 검증 |

## 첫 번째 공식 문서 묶음

- [공식 AI 문서 인덱스](https://docs.tosspayments.com/llms.txt): 현재 문서 집합과 주제 탐색의 시작점
- [결제 흐름 이해하기](https://docs.tosspayments.com/guides/v2/get-started/payment-flow): 요청·인증·승인과 승인 전 검증
- [코어 API](https://docs.tosspayments.com/reference): Payment 객체, 결제 상태, 승인·조회·취소
- [인증 및 기타 헤더 설정](https://docs.tosspayments.com/reference/using-api/authorization): 인증과 `Idempotency-Key`
- [가상계좌 결제](https://docs.tosspayments.com/guides/v2/payment-window/integration-virtual-account): 발급·입금·취소 규칙
- [웹훅](https://docs.tosspayments.com/guides/v2/webhook): 성공 판정과 재전송 정책
- [웹훅 이벤트](https://docs.tosspayments.com/reference/using-api/webhook-events): 상태 변경 알림
- [정산](https://docs.tosspayments.com/resources/glossary/settlement): 상점 정산의 액터와 금액·주기 개념

레거시 개선 시리즈는 [근거 지도](TOSS_PAYMENTS_EVIDENCE_MAP.md)의 case-study 섹션을 통해서만 사용합니다. 프로젝트 01의 첫 적용은 [결제·원장 도메인 브리프](../projects/01-ledger-core/DOMAIN_BRIEF.md)에서 시작합니다.
