---
paths:
  - "nextup-core/**/*.kt"
---

# Core Layer Rules

> nextup-core 모듈 전용 규칙. 도메인 엔티티, 비즈니스 로직, Value Object에 적용.

## 의존성 제한 (절대)
- `com.nextup.infrastructure` 패키지 import 금지
- `com.nextup.api` 패키지 import 금지
- `com.nextup.backoffice` 패키지 import 금지
- `com.nextup.scorer` 패키지 import 금지
- Spring Framework 어노테이션은 JPA/Validation 관련만 허용 (`@Entity`, `@Column`, `@Valid` 등)

## Rich Domain Model 필수
- Entity는 반드시 `private constructor` + `companion object.create()` 팩토리 패턴
- 비즈니스 로직은 Entity 내부 메서드에 캡슐화 (Service에서 상태 직접 변경 금지)
- `var` 최소화, 상태 변경은 비즈니스 메서드를 통해서만
- `BaseTimeEntity` 상속 필수

## Value Object 패턴
- 복합 값은 `@Embeddable data class`로 정의
- `init` 블록에서 불변식(invariant) 검증
- 불변(immutable) 설계 필수

## Port 인터페이스
- Repository는 Port 인터페이스(`*RepositoryPort`)로 정의
- 구현체는 `nextup-infrastructure`에 위치
