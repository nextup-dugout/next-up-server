# /tdd - TDD Workflow

TDD(Test-Driven Development) 워크플로우를 활성화합니다.

## Workflow

```
1. Write test first (RED)
   - 실패하는 테스트 작성
   - 테스트가 실패하는지 확인

2. Run test - it should FAIL
   - ./gradlew test
   - 예상대로 실패하는지 확인

3. Write minimal implementation (GREEN)
   - 테스트를 통과하는 최소한의 코드 작성
   - 오버엔지니어링 금지

4. Run test - it should PASS
   - ./gradlew test
   - 테스트 통과 확인

5. Refactor (IMPROVE)
   - 코드 품질 개선
   - 중복 제거
   - 네이밍 개선

6. Verify coverage (80%+)
   - ./gradlew jacocoTestReport
   - 커버리지 80% 이상 확인
```

## TDD 필수 적용 대상

- ✅ `nextup-core/Entity` 비즈니스 로직
- ✅ `nextup-core/Service` 계층
- ✅ `nextup-core/Domain Events`
- ✅ `nextup-core/Value Objects`

## TDD 선택 적용 (AI 판단)

- 🟡 Controller (주로 통합 테스트)
- 🟡 Repository (QueryDSL 테스트)
- 🟡 DTO/Mapper (단순 변환)
- 🟡 Configuration

## 사용 예시

```
User: /tdd
Assistant: TDD 워크플로우를 시작합니다. 구현할 기능을 설명해주세요.

User: Game 엔티티에 cancel 메서드 추가
Assistant:
1. [RED] 먼저 실패하는 테스트를 작성합니다...
   GameTest.kt에 테스트 추가

2. 테스트 실행하여 실패 확인...
   ./gradlew :nextup-core:test

3. [GREEN] 최소한의 구현 작성...
   Game.kt에 cancel 메서드 추가

4. 테스트 실행하여 통과 확인...

5. [REFACTOR] 필요시 리팩토링...

6. 커버리지 확인...
```

## 활용 Skills

- `backend-patterns`: 테스트 작성 패턴
- `quality-metrics`: 커버리지 검증

## 활용 Agents

- `architect`: Entity 설계 시 TDD 적용
- `implementer`: Service/Controller 구현 시 TDD 적용
- `reviewer`: 테스트 커버리지 검수
