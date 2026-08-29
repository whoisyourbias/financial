# 프로젝트 07 — API 호환 마이그레이션

## 목표

동일한 선택 공개 계약 뒤에서 기준 엔진을 새 내부 결제 엔진으로 교체하되, shadow 비교·점진 전환·롤백으로 상점 계약과 금액 불변식을 지킵니다.

## 전제

이 프로젝트의 “레거시”는 학습을 위해 저장소 안에 만든 기준 구현입니다. 토스페이먼츠의 실제 과거 시스템이나 마이그레이션을 재현했다고 주장하지 않습니다.

## 범위

- 승인·조회·취소 공개 계약의 고정 contract suite
- 기준 엔진과 새 엔진의 공통 application port
- 요청·응답 정규화 어댑터
- 읽기 전용 shadow 실행과 차이 분류
- MID 단위 점진 라우팅
- 데이터 backfill과 검증
- 즉시 롤백과 샘플 상점 무변경 검증

## 불변식

1. 공개 경로, 인증 방식, 선택 필드, 오류 매핑은 전환 전후 동일합니다.
2. shadow 엔진은 원장·웹훅·외부 응답에 부작용을 만들지 않습니다.
3. 차이는 필드 순서 같은 허용 차이와 금액·상태 같은 차단 차이로 분리합니다.
4. 한 결제 명령은 활성 엔진 한 곳만 권위 있게 커밋합니다.
5. backfill은 재실행 가능하고 원본을 덮어쓰지 않습니다.
6. 롤백 이후에도 새로 생성된 결제를 조회·취소할 호환 경로가 있습니다.

## 단계

1. 현재 계약과 golden fixture를 기준선으로 고정합니다.
2. 공통 port와 기준 엔진 adapter를 분리합니다.
3. 새 엔진과 데이터 변환기를 구현합니다.
4. 합성 트래픽을 shadow로 실행해 semantic diff를 수집합니다.
5. 차단 차이 0과 데이터 검증 통과 후 MID 일부를 전환합니다.
6. 오류율·지연·금액 불일식 임계치에서 자동 또는 수동 롤백합니다.

## 차이 분류

- `IGNORED`: 필드 순서, 생성 시각 허용 오차
- `REVIEW`: 문서상 선택 필드의 표현 차이
- `BLOCKING`: amount, balanceAmount, status, cancelAmount, 오류 코드, 분개 합계
- `UNKNOWN`: 규칙이 없어 사람이 분류해야 하는 차이

## 검증 산출물

- 전환 전후 동일 contract suite 결과
- shadow 무부작용 테스트
- semantic diff 원시 데이터와 분류 규칙
- backfill 재실행·체크섬 보고서
- canary·롤백 타임라인
- `evidence/projects/07/` manifest

## 근거

- source refs: `toss-payments-versioning`, `toss-payments-request-response`, `toss-payments-error-codes`, `toss-payments-core-api`
- 사례 연구: `toss-legacy-intro`, `toss-legacy-open-api`, `toss-legacy-configuration`에서 질문을 얻되 실제 내부 구조를 모방했다고 표현하지 않습니다.
