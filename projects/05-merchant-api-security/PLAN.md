# 프로젝트 05 — 상점 API 보안과 감사

## 목표

합성 MID와 테스트 API 키를 사용해 상점 경계를 강제하고, 키 수명주기·권한·감사 로그를 재현 가능한 보안 시나리오로 검증합니다.

## 범위

- 시크릿 키 기반 HTTP Basic 인증
- MID·환경(test/live-simulated) 격리
- 키 발급·해시 저장·회전·폐기·유예 기간
- 최소 권한 역할: `MERCHANT_DEVELOPER`, `MERCHANT_OPERATOR`, `PLATFORM_OPERATIONS`, `AUDITOR`, `ADMIN`
- 보안 관련 오류 응답과 rate limit
- 변경 불가능한 감사 이벤트
- 로그·트레이스·증거물의 자격증명 마스킹

`live-simulated`는 동작 차이를 연습하는 이름일 뿐 실제 라이브 결제망이 아닙니다.

## 불변식

1. 원문 시크릿은 생성 응답 이후 저장·로그·추적하지 않습니다.
2. 모든 결제·웹훅·정산 조회는 인증된 MID 범위를 벗어나지 못합니다.
3. 권한 거부와 키 수명주기 변경은 감사 이벤트를 남깁니다.
4. 키 폐기 이후 요청은 캐시 유무와 관계없이 거절됩니다.
5. 테스트 키와 live-simulated 키는 교차 사용할 수 없습니다.
6. 인증 문자열과 오류 형태는 선택한 공개 계약 근거가 있는 범위만 호환으로 표기합니다.

## 구현 순서

1. 위협 모델과 데이터 분류표를 작성합니다.
2. Basic 인증 어댑터와 키 저장 모델을 구현합니다.
3. MID 스코프를 application port와 repository query에 강제합니다.
4. 역할·운영 API·감사 이벤트를 구현합니다.
5. 키 회전 및 즉시 폐기 시나리오를 검증합니다.
6. 로그 스캔과 의존성·정적 분석을 CI에 연결합니다.

## 공격 시나리오

- 다른 MID의 paymentKey/orderId 추측 조회
- 폐기된 키의 캐시 재사용
- Basic 헤더의 잘못된 인코딩과 누락된 콜론
- 로그·예외·metric label로 시크릿 유출
- 권한 없는 웹훅 수동 재전송과 환불 시도
- rate limit 우회

## 검증 산출물

- 위협 모델과 역할-권한 표
- MID 격리 통합 테스트
- 키 회전·폐기 테스트
- 감사 로그 변조 탐지 테스트
- secret/PII 스캔 결과와 `evidence/projects/05/` manifest

## 근거

- source refs: `toss-payments-api-keys`, `toss-payments-secret-key-practice`, `toss-payments-security`, `toss-payments-environment`, `toss-payments-error-codes`
- 사례 연구: `toss-legacy-zero-trust`는 위협 질문의 참고이며 구현 사실의 증거가 아닙니다.
