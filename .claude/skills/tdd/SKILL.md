---
name: tdd
description: |
  TDD(Test-Driven Development) 워크플로우 활성화. RED-GREEN-REFACTOR 사이클을 안내하고
  대상 클래스/메서드에 대한 테스트 우선 개발을 수행한다.
user-invocable: true
argument-hint: "[ClassName.methodName] e.g. Game.cancel"
allowed-tools: Bash, Read, Write, Edit, Glob, Grep
---

# /tdd - TDD Workflow

TDD(Test-Driven Development) 워크플로우를 활성화합니다.

## Arguments

`$ARGUMENTS`를 통해 대상 클래스/메서드를 지정할 수 있습니다.

| 사용법 | 설명 |
|--------|------|
| `/tdd` | TDD 모드 활성화 (대상 입력 대기) |
| `/tdd Game` | Game 엔티티 대상 TDD |
| `/tdd GameService.cancelGame` | 특정 메서드 대상 TDD |
| `/tdd BattingRecord` | BattingRecord 엔티티 대상 TDD |

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

- `nextup-core/Entity` 비즈니스 로직
- `nextup-core/Service` 계층
- `nextup-core/Domain Events`
- `nextup-core/Value Objects`

## TDD 선택 적용 (AI 판단)

- Controller (주로 통합 테스트)
- Repository (QueryDSL 테스트)
- DTO/Mapper (단순 변환)
- Configuration

