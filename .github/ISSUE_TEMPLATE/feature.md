---
name: Feature Request
about: 새로운 기능 또는 명세가 있나요?
labels: enhancement
---

## 💡 기능 설명
<!-- 새로운 기능에 대한 명확한 설명을 작성해 주세요 -->
<!-- 어떤 문제를 해결하거나 어떤 가치를 제공하나요? -->

**예시:**
> 선수의 타순을 경기 중에 변경할 수 있는 API가 필요합니다.
> 현재는 경기 시작 전에만 타순을 설정할 수 있어서, 경기 중 전략 변경이 불가능합니다.

## 🎯 목표
<!-- 이 기능으로 달성하고자 하는 목표를 작성해주세요 -->

**예시:**
- 경기 중 타순 변경 가능
- 변경 이력 추적 가능
- 야구 규칙(1-9번 타순) 검증

## 📋 작업 내용
<!-- 구현해야 할 작업을 최대한 세분화하여 작성해 주세요 -->

**예시:**
- [ ] Player Entity에 `changeBattingOrder()` 메서드 추가
- [ ] `PATCH /api/v1/players/{id}/batting-order` 엔드포인트 구현
- [ ] ChangeBattingOrderRequest DTO 추가
- [ ] 타순 변경 이력 테이블 설계 및 구현
- [ ] 단위 테스트 작성 (커버리지 80% 이상)
- [ ] 통합 테스트 작성
- [ ] API 문서 작성

## 📐 기대 API 명세
<!-- API 스펙이 있다면 작성해주세요 -->

**예시:**
```http
PATCH /api/v1/players/{id}/batting-order
Content-Type: application/json

{
  "battingOrder": 3
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "홍길동",
    "battingOrder": 3
  }
}
```

## 🔗 참고 자료
<!-- 관련 문서, 이슈, 레퍼런스 등이 있다면 추가해주세요 -->

**예시:**
- KBO 규칙: https://...
- 관련 이슈: #12
