# API·이벤트 계약 소유권

OpenAPI와 event schema는 해당 application boundary가 소유합니다. 아직 존재하지 않는 endpoint나 event를 계획만으로 생성하지 않습니다.

## 버전 규칙

- 호환 가능한 field 추가를 우선합니다.
- 기존 의미를 바꾸거나 필드를 제거하는 변경은 새 major contract version으로 발행합니다.
- client는 사용자 메시지가 아니라 표준 오류 `code`로 분기합니다.
- schema source만 추적하고 generated client·문서는 재생성 가능할 때 build output으로 둡니다.
- backend 구현이 추가되면 contract lint와 provider test를 `harnessFull`에 연결합니다.
