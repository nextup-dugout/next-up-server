---
paths:
  - "nextup-scorer/**/*.kt"
---

# Scorer Layer Rules

> nextup-scorer 모듈 전용 규칙. 실시간 경기 기록 (port 8082, WebSocket).

## WebSocket 패턴
- 메시지 클래스는 `dto/websocket/` 패키지에 위치
- STOMP 프로토콜 사용
- 메시지 타입별 Handler 분리
- WebSocket 세션 인증: Handshake 시 JWT 토큰 검증 필수
- 구독 경로: `/topic/game/{gameId}` 패턴
- 발행 경로: `/app/game/{gameId}/event` 패턴

## URL 패턴
- URL prefix: `/api/scorer/`
- RESTful 리소스 명명: 복수형 (`/api/scorer/games`, `/api/scorer/lineups`)

## 네이밍 컨벤션
- Controller: `*ScorerController` 패턴
- WebSocket Handler: `*WebSocketHandler` 패턴
- WebSocket Config: `WebSocketConfig` (단일)
- 메시지 DTO: `*Message`, `*Payload` 패턴

## 모듈 독립성
- `nextup-api` 모듈 참조 금지
- `nextup-backoffice` 모듈 참조 금지
- 공통 로직은 `nextup-infrastructure`에 위치

## 실시간 처리
- 경기 이벤트는 도메인 이벤트로 전파
- 낙관적 잠금(`@Version`)으로 동시성 제어
- 연결 끊김 시 재접속 지원 (세션 상태 복원)

## 에러 처리
- WebSocket 에러는 `/user/queue/errors`로 개별 전송
- 잘못된 이벤트 요청 시 STOMP ERROR 프레임 반환
- 세션 타임아웃: 30분 (비활성 기록원 자동 해제)
