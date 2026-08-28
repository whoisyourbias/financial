# 도메인 지식 문서 작업 지침

- 새 검색 대상은 `catalog.yaml`에 고유하고 변경하지 않는 ID로 등록합니다.
- `[검증됨]` 또는 `verified`는 공식 `sourceRefs`나 재현 가능한 `evidenceRefs` 없이는 사용할 수 없습니다.
- 외부 원문을 저장소에 복제하지 않고 `sources.yaml`에 공식 URL, 발행 기관, 확인일과 적용 범위만 기록합니다.
- 계획, 구현 결과, 현재 한계를 같은 상태로 섞지 않습니다.
- 내부 링크와 heading을 변경하면 `./gradlew knowledgeCheck`를 실행합니다.
- export 결과인 `build/knowledge/knowledge.jsonl`은 추적하지 않습니다.
