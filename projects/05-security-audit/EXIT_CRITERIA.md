# 프로젝트 05 종료 기준

## 기능·보안 게이트

- [ ] JWT issuer·audience·signature·expiry가 검증됩니다.
- [ ] authorization matrix가 역할과 리소스 소유권을 함께 강제합니다.
- [ ] 권한 없는 조회·변경이 domain service에서도 차단됩니다.
- [ ] 정의한 민감 필드가 저장·응답·로그·trace 정책을 따릅니다.
- [ ] 변경·조회·거절의 actor·resource·outcome·correlation이 audit에 남습니다.
- [ ] audit·암호화 실패 의미와 처리 정책이 문서화됐습니다.
- [ ] repository와 evidence에 secret·실제 PII가 없습니다.

## 테스트·증거 게이트

- [ ] authorization matrix 각 행에 자동화 테스트가 연결됩니다.
- [ ] 허용·거절 결과와 audit record가 같은 correlation ID로 대조됩니다.
- [ ] masking 전 원문이 공개 artifact에 남지 않습니다.
- [ ] encryption key 오류와 residual risk가 기록돼 있습니다.
- [ ] STRIDE threat와 구현 통제·미통제 위험이 연결됩니다.
- [ ] secret·PII scan 명령과 결과가 manifest에 있습니다.
- [ ] 보안 인증·규제 준수를 주장하지 않는 비범위가 공개돼 있습니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                           |
| --------------- | --------------------------------------------------- |
| 기술적 의사결정 | 권한 강제 위치, 암호화 경계, audit 실패 의미와 비용 |
| 문제 해결       | 역할+소유권 우회, 유출, 키·audit 실패를 테스트      |
| 성과·임팩트     | matrix coverage와 유출·거절·audit 대조 원본         |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] threat model과 실제 통제가 일치합니다.
- [ ] 대표 보안 주장에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] `RBAC 구현`을 전체 보안 완성으로 표현하지 않았습니다.
- [ ] 암호화 필드와 평문 metadata를 구분했습니다.
- [ ] 마스킹 sample만 보고 원본 로그 전체가 안전하다고 일반화하지 않았습니다.
- [ ] 합성 PII를 실제 개인정보 처리 경험으로 표현하지 않았습니다.
- [ ] ISMS·PCI DSS·금융 규제 준수 표현을 사용하지 않았습니다.
- [ ] latency 변화가 여러 보안 변경의 합성 효과라면 단일 원인으로 귀속하지 않았습니다.

## 면접 방어

- [ ] 역할과 리소스 소유권의 차이를 설명할 수 있습니다.
- [ ] audit 실패 정책의 트레이드오프를 설명할 수 있습니다.
- [ ] field encryption의 key·검색·rotation 비용을 설명할 수 있습니다.
- [ ] 보호하지 못한 threat와 이유를 말할 수 있습니다.
- [ ] 인증과 인가, 감사와 도메인 이벤트를 구분할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록        |
| ---------------- | ---------------------------- |
| Portfolio Review | PASS 또는 REJECT             |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수 |
| Residual risk    | 공개된 미통제 위험           |
| Evidence         | commit SHA와 manifest 경로   |
| 최종 상태        | RELEASED 또는 HOLD           |

권한 우회, 민감정보 유출, audit 서사 불일치 또는 🔴가 남으면 태그를 생성하지 않습니다.
