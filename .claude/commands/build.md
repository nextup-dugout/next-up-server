# /build Command - 빌드 & 테스트 실행

> **Gradle 빌드 + JUnit 테스트** 한 번에 실행

## 목적

코드 변경 후 빌드 성공 여부와 테스트 통과 여부를 즉시 확인합니다.

## 사용법

```
/build
```

## 실행 흐름

### 1. Gradle 빌드
```bash
./gradlew clean build
```

### 2. 결과 분석

#### 성공 시
```
BUILD SUCCESSFUL in 1m 23s

✅ 빌드 성공
✅ 테스트 통과
```

#### 실패 시 - 컴파일 에러
```
Unresolved reference: PlayerRepository

원인: import 누락
조치: implementer에게 import 추가 요청
```

#### 실패 시 - 테스트 실패
```
PlayerTest > 타순을 변경할 수 있다() FAILED
Expected: 3
Actual: null

원인: 비즈니스 로직 오류
조치: implementer에게 수정 요청
```

### 3. 커버리지 리포트 생성
```bash
./gradlew jacocoTestReport

리포트 위치:
- nextup-core: build/reports/jacoco/test/html/index.html
- nextup-api: build/reports/jacoco/test/html/index.html
```

## Skills 참조

- **quality-metrics**: Gradle 빌드 & Jacoco
- **tdd**: 테스트 작성 가이드

## Agents 협업

- **implementer**: 빌드 실패 시 수정
- **reviewer**: 빌드 성공 시 검수

## 체크리스트

- [ ] 빌드 성공
- [ ] 테스트 통과
- [ ] Jacoco 리포트 생성

## 예시

```
User: "/build"

Agent:
1. Gradle 빌드 실행
   ./gradlew clean build

2. 결과 분석
   ✅ BUILD SUCCESSFUL
   ✅ 45개 테스트 통과

3. 커버리지 리포트
   - nextup-core: 82%
   - nextup-api: 71%
   - 전체: 78.5%

4. 다음 단계 제안
   "/review를 실행하여 보안 검증하세요"
```

## 이 Command의 장점

- ✅ 빠른 빌드 검증
- ✅ 테스트 실패 즉시 확인
- ✅ 커버리지 리포트 자동 생성
