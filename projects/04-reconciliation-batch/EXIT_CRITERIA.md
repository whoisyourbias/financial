# 프로젝트 04 종료 기준

## 기능·정합성 게이트

- [ ] file schema·checksum·cut-off가 검증됩니다.
- [ ] 동일 파일 재처리가 중복 결과를 만들지 않습니다.
- [ ] 다섯 차이 유형이 원본 internal·external ID와 함께 분류됩니다.
- [ ] chunk 실패 뒤 안전하게 재시작됩니다.
- [ ] 수동 해결이 차이 record를 삭제하거나 원장을 직접 수정하지 않습니다.
- [ ] 개별 matched 합계와 보고서 합계가 같습니다.
- [ ] cut-off 이후 거래가 현재 batch에 포함되지 않습니다.

## 테스트·증거 게이트

- [ ] generator seed·file checksum·record 수가 기록돼 있습니다.
- [ ] job·step 실행 상태와 restart 원본 query가 있습니다.
- [ ] crash 지점과 restart 시작점이 확인됩니다.
- [ ] 중복 파일 실행 전후 difference·report 수가 대조됩니다.
- [ ] 차이 유형별 개수·금액·통화 분모가 일치합니다.
- [ ] timezone과 cut-off query가 문서·코드에 일치합니다.
- [ ] chunk 크기와 transaction 범위가 evidence에 있습니다.

## Portfolio Review 평가표

| 영역            | PASS 근거                                      |
| --------------- | ---------------------------------------------- |
| 기술적 의사결정 | 대사 key·chunk transaction·restart 전략과 비용 |
| 문제 해결       | 부분·중복·불일치·cut-off 실패를 분류하고 복구  |
| 성과·임팩트     | 개별·합계 대사와 restart 결과의 원본 정합      |

- [ ] 세 영역 모두 `적합` 이상입니다.
- [ ] 실제 파일 처리 범위를 과장하지 않았습니다.
- [ ] report·본문·query에 🔴 결함이 없습니다.

## Red Team 공격 목록

- [ ] `대사 차이 탐지`를 `정산 자동 완료`로 바꿔 쓰지 않았습니다.
- [ ] batch enqueue 시간과 전체 완료 시간을 구분했습니다.
- [ ] 합계 일치만으로 개별 대사 성공을 주장하지 않았습니다.
- [ ] 파일명과 checksum의 역할을 혼동하지 않았습니다.
- [ ] 처리 건수를 대용량 경험으로 표현하지 않았습니다.
- [ ] chunk 변경과 다른 최적화를 한 원인으로 묶지 않았습니다.

## 면접 방어

- [ ] job instance·execution·step execution 차이를 설명할 수 있습니다.
- [ ] restart 시 중복 writer 결과를 막는 방식을 설명할 수 있습니다.
- [ ] 차이 유형별 운영 대응을 설명할 수 있습니다.
- [ ] cut-off·영업일·timezone 한계를 설명할 수 있습니다.
- [ ] 자동 금전 보정을 하지 않은 이유를 설명할 수 있습니다.

## 판정 기록

| 항목             | 프로젝트 종료 시 기록        |
| ---------------- | ---------------------------- |
| Portfolio Review | PASS 또는 REJECT             |
| Red Team         | 🔴 / 🟠 / 🟡 / `[밋밋]` 건수 |
| Restart 결과     | 실패 지점과 복구 근거        |
| Evidence         | commit SHA와 manifest 경로   |
| 최종 상태        | RELEASED 또는 HOLD           |

재시작이 중복 결과를 만들거나 개별·합계 대사가 어긋나거나 🔴가 남으면 태그를 생성하지 않습니다.
