# 프로젝트 08 — 상점 정산 엔진

## 목표

완료·취소된 합성 결제에서 상점별 정산 원장을 만들고, 수수료 정책 스냅샷·반올림·영업일·사후 조정을 재현 가능한 배치로 처리합니다.

## 필수 범위

- 정산 대상 결제 스냅샷
- 한 합성 상점의 versioned 수수료 정책
- 총액, 수수료, 세금, 지급 예정액 계산
- 고정 소규모 영업일 fixture의 정산일 계산
- 대표 사후 부분 취소 1건의 차기 정산 adjustment
- 재시작 가능한 확정 배치

## 확장 범위

- 다중 상점·결제수단·시점별 수수료 정책
- 전체 공휴일·월말 calendar, 지급액보다 큰 음수 이월
- `/sandbox/ops/settlements/**` 운영 조회와 UI

확장 운영 API를 구현하더라도 토스페이먼츠 정산 API 호환 범위가 아닙니다. 필수 산식·snapshot·restart·대표 adjustment가 끝나기 전에는 확장을 시작하지 않습니다.

## 불변식

1. 입력·출력·저장은 `long amountMinor`를 사용합니다. 수수료·세금 나눗셈의 중간 계산만 `BigDecimal`과 이름 있는 반올림 정책을 사용하고 검증된 범위 안에서 다시 `long`으로 변환합니다.
2. 각 정산 라인은 계산 당시 수수료 정책 버전을 스냅샷으로 보존합니다.
3. 같은 결제·정산 주기에 대한 라인은 중복 확정되지 않습니다.
4. 확정된 정산은 수정하지 않고 adjustment로 보정합니다.
5. 총 결제액, 취소액, 수수료, 세금, 지급액의 산술 관계를 속성 테스트로 검증합니다.
6. 재시작은 완료 chunk를 중복 반영하지 않습니다.
7. 실제 가맹점 계약 수수료나 토스페이먼츠 내부 산식을 사용했다고 주장하지 않습니다.

## 구현 순서

1. 합성 수수료·세금·영업일 정책과 예제를 고정합니다.
2. settlement candidate와 policy snapshot 모델을 구현합니다.
3. 계산 엔진과 원장 분개를 구현합니다.
4. Spring Batch 확정·재시작·부분 실패를 구현합니다.
5. 대표 사후 부분 취소 adjustment와 다음 지급 상계를 구현합니다.
6. 필수 검증 뒤 확장으로 운영 조회·대사·승인 런북을 추가할 수 있습니다.

## 경계 사례

- 수수료 정책이 결제 후 변경됨
- 고정 영업일 fixture의 주말 경계
- 부분 취소가 정산 전/후 발생
- 확장: 지급 예정액보다 큰 사후 조정
- batch chunk 커밋 직후 프로세스 종료
- 같은 입력 파일 또는 스냅샷 재수행

## 검증 산출물

- 산식 truth table과 속성 테스트
- 정책 버전 변경 회귀 테스트
- batch restart/checkpoint 테스트
- adjustment와 정산 원장 대사
- 합성 데이터 출처·seed·manifest
- `projects/08-settlement-engine/evidence/`

## 근거

- source refs: `toss-payments-settlement`, `toss-payments-data-types`
- 사례 연구: `toss-legacy-settlement`, `toss-legacy-ledger`는 문제 구조의 참고 자료이며 실제 정책의 근거가 아닙니다.
