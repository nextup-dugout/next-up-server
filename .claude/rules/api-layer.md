---
paths:
  - "nextup-api/**/*.kt"
---

# API Layer Rules

> nextup-api 모듈 전용 규칙. 일반 사용자용 공개 API (port 8080).

## Zero Entity Leak (절대)
- Controller 반환타입에 Entity 직접 사용 금지
- 반드시 `*Response` DTO로 변환 후 반환
- Mapper 또는 Extension Function으로 변환

## ApiResponse 래핑 (필수)
- 모든 Controller 응답은 `ApiResponse<T>`로 래핑
- 성공: `ApiResponse.success(data)`
- 실패: `ApiResponse.error(code, message)`

## 입력 검증
- `@RequestBody`에 `@Valid` 어노테이션 필수
- Request DTO에 Jakarta Validation 어노테이션 사용

## 모듈 독립성
- `nextup-backoffice`, `nextup-scorer` 모듈 참조 금지
- 공통 로직은 `nextup-infrastructure`에 위치
