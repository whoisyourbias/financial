# Frontend 작업 지침

- framework와 dependency는 프로젝트 06 ADR 전에는 추가하지 않습니다.
- versioned OpenAPI와 표준 오류 `code`를 client 계약의 source of truth로 사용합니다.
- 인증 실패와 권한 거절을 구분하고 내부 오류·SQL·개인정보를 화면에 노출하지 않습니다.
- keyboard navigation, focus order, accessible name과 주요 사용자 흐름을 browser test에 포함합니다.
- generated API client는 생성 명령과 source schema를 기록하고 수동 수정하지 않습니다.
