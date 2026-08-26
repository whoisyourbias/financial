# 프로젝트 05 — 보안·권한·감사

| 항목           | 값                                                   |
| -------------- | ---------------------------------------------------- |
| 기간           | 2026-10-26 ~ 2026-11-08                              |
| 상태           | PLANNED                                              |
| 선행 프로젝트  | 01~04                                                |
| 목표 태그      | `p05-security-audit`                                 |
| 주력 채용 신호 | 인증·인가, 개인정보 보호, 감사 추적, threat modeling |

## 문제 정의

금전 기능이 정확해도 권한 없는 사용자가 원장을 조회하거나 대사 차이를 해결할 수 있다면 안전한 시스템이 아닙니다. 또한 감사 로그에 민감정보가 남거나 행위 주체와 correlation이 누락되면 사후 조사도 어렵습니다. 이 프로젝트는 역할과 리소스 범위를 명시하고, 개인정보와 비밀정보가 API·로그·trace·AI 입력으로 새지 않도록 방어합니다.

이 프로젝트는 보안 인증이나 금융 규제 준수를 증명하지 않습니다. 정의한 위협 모델과 테스트 범위에서 구현한 통제를 검증합니다.

## 필수 범위

- OAuth2/JWT resource server와 로컬 identity provider
- 역할: `CUSTOMER`, `OPERATIONS`, `AUDITOR`, `ADMIN`
- 리소스 소유권과 역할을 함께 확인하는 authorization matrix
- 원장·이체·대사 API의 최소 권한
- 합성 고객 식별자의 저장·응답·로그 마스킹
- 민감 필드의 application-level encryption과 key ID 기록
- 변경·조회·권한 거절의 audit event
- actor, action, resource, outcome, occurredAt, correlationId
- secret·PII log scan과 repository scan
- STRIDE 기반 핵심 data flow threat model

## 비범위

- 실제 주민등록번호·계좌번호 처리
- 금융보안원 인증, ISMS, PCI DSS 준수 주장
- 자체 인증 서버 구현
- 완전한 SIEM·SOC 구축
- 암호키 rotation 자동화 완성
- audit log를 법적 증거로 주장

## 권한 규칙

| 행위             | CUSTOMER       | OPERATIONS     | AUDITOR   | ADMIN       |
| ---------------- | -------------- | -------------- | --------- | ----------- |
| 본인 계정 조회   | 허용           | 업무 범위 허용 | 읽기 허용 | 허용        |
| 이체 생성        | 본인 계정      | 금지           | 금지      | 제한적 허용 |
| journal 조회     | 본인 범위 요약 | 업무 범위      | 읽기 허용 | 허용        |
| 대사 차이 조회   | 금지           | 허용           | 읽기 허용 | 허용        |
| 대사 차이 해결   | 금지           | 허용           | 금지      | 허용        |
| 사용자 역할 변경 | 금지           | 금지           | 금지      | 허용        |
| audit 조회       | 본인 행위 제한 | 업무 범위      | 허용      | 허용        |

구현 시 리소스 소유권을 확인하지 않는 `역할만 맞으면 전체 조회`를 금지합니다.

## 설계 후보

### 권한 강제 위치

- endpoint annotation만 사용
- application service policy 사용
- repository query에 owner scope 포함

endpoint만 통제하면 내부 호출 우회 위험이 있고 repository만 통제하면 비즈니스 의도를 설명하기 어렵습니다. application policy를 중심으로 endpoint·query defense-in-depth를 실제 적용하고 중복 비용을 기록합니다.

### 민감정보 암호화

- DB 전체 암호화에만 의존
- application field encryption
- tokenization

합성 식별자 일부에 field encryption을 적용해 key ID·오류·검색 제한 비용을 확인합니다. 사용하지 않은 tokenization을 구현했다고 쓰지 않습니다.

## 데이터 흐름

```text
JWT
  → signature·issuer·audience·expiry 검증
  → actor·role·resource scope 구성
  → application authorization policy
  → domain command/query
  → masked response
  → audit outcome 기록
```

audit 기록 실패가 금전 transaction을 어떻게 처리할지는 ADR로 확정합니다. 보안 감사와 도메인 이벤트를 같은 로그로 취급하지 않습니다.

## 산출물

- authorization matrix와 policy tests
- JWT 검증 설정과 local identity fixture
- PII classification·masking·field encryption
- structured audit schema와 query
- STRIDE threat model·abuse case
- secret·log leakage 검사
- security decision ADR와 limitation

## 10일 작업 계획

| 일차 | 작업                         | 완료 기준                     |
| ---- | ---------------------------- | ----------------------------- |
| 1    | 자산·주체·위협·비범위        | data flow별 threat 명시       |
| 2    | 권한 matrix·암호화·audit ADR | policy와 실패 의미 확정       |
| 3    | JWT resource server          | issuer·audience·expiry reject |
| 4    | application authorization    | 역할+소유권 policy test       |
| 5    | masking·field encryption     | 평문 노출 경로 통제           |
| 6    | audit schema·interceptor     | 성공·거절·실패 outcome 기록   |
| 7    | abuse·secret·log tests       | 주요 우회·유출 case 자동화    |
| 8    | audit 누락·키 오류 실험      | 실패 의미와 복구 원본 생성    |
| 9    | threat model·evidence·한계   | 통제와 미통제 threat 연결     |
| 10   | Portfolio Review·Red Team    | PASS와 🔴 0건 또는 HOLD       |

## 테스트와 측정

- 만료·잘못된 issuer·audience·signature JWT
- 역할은 맞지만 다른 사용자 리소스 접근
- OPERATIONS·AUDITOR의 변경 권한 우회
- masking 전·후 응답과 로그·trace
- encryption key 누락·오류·잘못된 key ID
- audit actor·resource·correlation 누락
- audit 저장 실패
- log forging 문자열과 control character
- repository의 secret·fixture PII scan

성능 오버헤드는 측정할 수 있지만 대표 성과로 과장하지 않습니다. 정책·암호화 전후 조건을 동일하게 유지하지 못하면 latency 인과를 주장하지 않습니다.

## 필요한 증거

- authorization matrix와 자동화 test mapping
- 허용·거절 HTTP 결과와 audit record
- 마스킹된 API·log·trace sample
- 암호문, key ID, 복호화 실패 결과
- threat별 구현 통제와 residual risk
- secret·PII scan 명령과 결과
- 미구현 rotation·SIEM·인증 범위

## 예상 면접 질문

1. 역할만 확인하고 리소스 소유권을 확인하지 않으면 어떤 취약점이 생기나요?
2. audit 저장이 실패하면 금전 transaction도 실패시켜야 하나요?
3. field encryption은 검색·인덱스·키 rotation에 어떤 비용을 만듭니까?
4. 보안 로그 자체에 개인정보가 들어가는 문제를 어떻게 막나요?
5. 이 구현이 ISMS·PCI DSS 준수를 의미하지 않는 이유는 무엇인가요?

## 주요 위험과 다음 회차 연결

- `암호화했다`는 말만 쓰지 않고 보호 필드·key 경계·미보호 metadata를 밝힙니다.
- authorization test가 controller에만 머물지 않도록 service policy를 직접 호출합니다.
- audit append-only와 변조 방지를 법적 불변성으로 과장하지 않습니다.
- 프로젝트 06에서 security outcome을 metric·dashboard·runbook과 연결합니다.
