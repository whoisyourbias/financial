# 프로젝트 05 — 상점 API 보안과 감사

## 목표

합성 MID와 테스트 API 키를 사용해 상점 경계를 강제하고, 키 수명주기·권한·감사 로그를 재현 가능한 보안 시나리오로 검증합니다.

## 필수 범위

- 시크릿 키 기반 HTTP Basic 인증
- MID·환경(test/live-simulated) 격리
- 키 발급·해시 저장·회전·폐기·유예 기간
- 서비스 주체와 사람 운영자를 분리한 최소 권한 역할
- 보안 관련 오류 응답과 rate limit
- application trust boundary에서 append-only로 기록하고 hash chain으로 사후 변조를 탐지하는 감사 이벤트
- 로그·트레이스·증거물의 자격증명 마스킹

`live-simulated`는 동작 차이를 연습하는 이름일 뿐 실제 라이브 결제망이 아닙니다.

분산 quota 저장소, 외부 IdP 관리자 UI와 다중 조직 federation은 확장 범위이며 필수 인증·격리·rate-limit·감사 게이트가 끝나기 전에는 시작하지 않습니다.

## 인증 주체와 credential

| 주체 | 인증·credential | 허용 범위 |
| --- | --- | --- |
| 상점 서버 | MID·환경에 묶인 Basic API secret | 선정 공개 Payment API, 자기 MID만 |
| 사람 상점 운영자 | OAuth2/OIDC 로그인 뒤 short-lived JWT, `MERCHANT_DEVELOPER` 또는 `MERCHANT_OPERATOR` | 자기 MID 운영 API, 역할별 읽기·수동 조치 |
| 사람 플랫폼 운영자 | 별도 OAuth2/OIDC client와 MFA가 전제된 short-lived JWT, `PLATFORM_OPERATIONS`, `AUDITOR`, `ADMIN` | 승인된 운영 API; merchant secret 사용 금지 |
| 내부 worker | 배포 환경의 workload identity | 승인된 application port만, 사람 역할 impersonation 금지 |

JWT의 issuer·audience·expiry·role·MID claim을 검증하고, 상점 Basic key를 사람 로그인이나 플랫폼 권한에 재사용하지 않습니다.

## 불변식

1. 원문 시크릿은 생성 응답 이후 저장·로그·추적하지 않습니다.
2. 모든 결제·웹훅·정산 조회는 인증된 MID 범위를 벗어나지 못합니다.
3. 사람 운영 API는 유효한 issuer·audience·expiry와 역할·MID claim이 있는 JWT만 허용합니다.
4. 권한 거부와 키 수명주기 변경은 감사 이벤트를 남깁니다.
5. application API는 감사 이벤트 update/delete를 제공하지 않고 각 record는 이전 hash를 포함합니다. DB/root 관리자는 물리 변경이 가능하므로 절대 불변을 주장하지 않으며 독립 검증에서 변조를 탐지합니다.
6. 키 폐기 이후 요청은 캐시 유무와 관계없이 거절됩니다.
7. 테스트 키와 live-simulated 키는 교차 사용할 수 없습니다.
8. 인증 문자열과 오류 형태는 선택한 공개 계약 근거가 있는 범위만 호환으로 표기합니다.

## 구현 순서

1. 위협 모델과 데이터 분류표를 작성합니다.
2. Basic 인증 어댑터와 키 저장 모델을 구현합니다.
3. MID 스코프를 application port와 repository query에 강제합니다.
4. 사람 운영자 OIDC/JWT 검증, 역할·운영 API·감사 이벤트를 구현합니다.
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
- rate limit 정상·초과·주체 격리 테스트
- secret/PII 스캔 결과와 `projects/05-merchant-api-security/evidence/` manifest

## 근거

- source refs: `toss-payments-api-keys`, `toss-payments-secret-key-practice`, `toss-payments-security`, `toss-payments-environment`, `toss-payments-error-codes`
- 사례 연구: `toss-legacy-zero-trust`는 위협 질문의 참고이며 구현 사실의 증거가 아닙니다.
