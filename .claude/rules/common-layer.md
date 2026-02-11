---
paths:
  - "nextup-common/**/*.kt"
---

# Common Layer Rules

> nextup-common 모듈 전용 규칙. 공통 유틸리티, Exception 정의.

## 의존성 금지 (절대)
- Leaf 모듈로서 **어떤 모듈에도 의존하지 않음**
- Spring Framework 의존성 금지
- JPA/Hibernate 의존성 금지

## 허용 범위
- 유틸리티 클래스 (`DateTimeUtils`, `StringUtils` 등)
- 커스텀 Exception 클래스 (`BusinessException`, `*NotFoundException` 등)
- 공통 상수 정의

## 금지 사항
- 비즈니스 로직 금지
- Entity/DTO 정의 금지
- 외부 라이브러리 의존 최소화
