---
paths:
  - "nextup-backoffice/**/*.kt"
---

# Backoffice Layer Rules

> nextup-backoffice 모듈 전용 규칙. 관리자 CRUD (port 8081).

## 네이밍 컨벤션
- Controller: `*AdminController` 패턴
- DTO: `*AdminRequest`, `*AdminResponse` 패턴
- URL prefix: `/admin/api/v1/`

## 보안
- SecurityConfig 독립 구성 (nextup-api와 별도)
- 관리자 인증 필수

## 모듈 독립성
- `nextup-api` 모듈 참조 금지
- `nextup-scorer` 모듈 참조 금지
- 공통 로직은 `nextup-infrastructure`에 위치

## ApiResponse
- Backoffice 전용 `ApiResponse<T>` 사용 (nextup-api의 ApiResponse 참조 금지)
