# /build - Gradle Build & Test

Gradle 빌드 및 테스트를 실행하고 결과를 분석합니다.

## Commands

### Full Build
```bash
./gradlew clean build
```

### Test Only
```bash
./gradlew test
```

### Specific Module
```bash
./gradlew :nextup-core:test
./gradlew :nextup-infrastructure:test
./gradlew :nextup-api:test
```

### With Coverage
```bash
./gradlew test jacocoTestReport
```

### Quick Check (No Tests)
```bash
./gradlew compileKotlin
```

## Build Process

1. **Clean** - 이전 빌드 결과 삭제
2. **Compile** - Kotlin 소스 컴파일
3. **Test** - JUnit 테스트 실행
4. **Verify** - 정적 분석 (ktlint, detekt)
5. **Package** - JAR 생성

## 실패 대응

### 컴파일 에러
```
> Task :nextup-core:compileKotlin FAILED
```
- 에러 메시지 확인
- 파일:라인 확인
- 타입 불일치, import 누락 등 수정

### 테스트 실패
```
> Task :nextup-core:test FAILED
X tests completed, Y failed
```
- 실패 테스트 확인
- 테스트 로직 또는 구현 수정
- `./gradlew test --info` 로 상세 확인

### 의존성 문제
```
Could not resolve: com.example:library:1.0.0
```
- build.gradle.kts 확인
- 저장소 설정 확인
- 버전 확인

## 출력 위치

```
build/
├── classes/          # 컴파일된 클래스
├── reports/
│   ├── tests/        # 테스트 리포트
│   └── jacoco/       # 커버리지 리포트
└── libs/             # JAR 파일
```

## 사용 예시

```
User: /build
Assistant:
빌드를 실행합니다...

$ ./gradlew clean build

BUILD SUCCESSFUL in 45s
32 actionable tasks: 32 executed

✅ 빌드 성공
- 테스트: 156 passed
- 커버리지: 82%
- 경고: 0건
```

## 활용 Skills

- `quality-metrics`: 빌드/테스트/커버리지 검증

## 활용 Agents

- `reviewer`: 빌드 성공 여부 검수
