# Platform 작업 지침

- Java 21 toolchain과 Spring Boot BOM을 사용하고 개별 managed dependency version을 덮어쓰지 않습니다.
- 의존 방향은 `bootstrap → ledger → shared-kernel`입니다.
- `bootstrap`에는 조립과 실행 설정만 두고 금융 domain rule을 넣지 않습니다.
- JPA entity와 repository는 소유 module 밖으로 공개하지 않고 public application service로 접근합니다.
- transaction 경계는 public application service에 두고 repository 호출마다 잘게 나누지 않습니다.
- `Money`는 signed value를 표현할 수 있지만 posting 금액의 양수 제약은 posting domain에서 검증합니다.
- 모든 schema 변경은 순서가 보존되는 Flyway migration으로 추가합니다. 적용된 migration을 수정하지 않습니다.
- database test는 PostgreSQL Testcontainers로 실행하고 H2를 대체품으로 사용하지 않습니다.
- 변경 후 최소 `./gradlew harnessFast`, DB·migration 변경은 `./gradlew harnessFull`을 실행합니다.
