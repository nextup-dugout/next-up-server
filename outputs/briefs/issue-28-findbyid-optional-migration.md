# Issue #28: Port findById() Optional 타입 마이그레이션

## 개요
Port 인터페이스와 JpaRepository의 findById 메서드 시그니처 충돌 해결

## 문제
- **Port**: `fun findById(id: Long): Entity?`
- **JpaRepository**: `fun findById(id: Long): Optional<Entity>`

JpaRepository를 직접 구현하는 Infrastructure 레이어에서 시그니처 불일치로 인한 컴파일 오류 발생

## 해결 방법
모든 Port 인터페이스의 `findById()` 메서드를 `Optional<T>` 반환 타입으로 변경

---

## 변경된 파일 목록

### 1. Port 인터페이스 (17개)

모든 Port 파일에서 다음과 같이 수정:

```kotlin
// Before
fun findById(id: Long): Entity?

// After  
import java.util.Optional
fun findById(id: Long): Optional<Entity>
```

**수정된 파일:**
1. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/AssociationRepositoryPort.kt`
2. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/BattingRecordRepositoryPort.kt`
3. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/CareerBattingStatsRepositoryPort.kt`
4. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/CareerPitchingStatsRepositoryPort.kt`
5. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/CompetitionRepositoryPort.kt`
6. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/GamePlayerRepositoryPort.kt`
7. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/LeagueRepositoryPort.kt`
8. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/OAuthAccountRepositoryPort.kt`
9. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/OrganizationAdminRepositoryPort.kt`
10. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/PitchingRecordRepositoryPort.kt`
11. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/PlayerRepositoryPort.kt`
12. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/PlayerTeamHistoryRepositoryPort.kt`
13. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/RefreshTokenRepositoryPort.kt`
14. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/SeasonBattingStatsRepositoryPort.kt`
15. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/SeasonPitchingStatsRepositoryPort.kt`
16. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/TeamRepositoryPort.kt`
17. `/nextup-core/src/main/kotlin/com/nextup/core/port/repository/UserRepositoryPort.kt`

### 2. Service 레이어 (8개)

Elvis 연산자(`?:`)를 `.orElseThrow()` 패턴으로 변경:

```kotlin
// Before
val entity = repository.findById(id)
    ?: throw EntityNotFoundException(id)

// After
val entity = repository.findById(id)
    .orElseThrow { EntityNotFoundException(id) }
```

**수정된 파일:**
1. `/nextup-core/src/main/kotlin/com/nextup/core/service/admin/OrganizationAdminService.kt` (2곳)
2. `/nextup-core/src/main/kotlin/com/nextup/core/service/association/AssociationService.kt` (1곳)
3. `/nextup-core/src/main/kotlin/com/nextup/core/service/competition/CompetitionService.kt` (2곳)
4. `/nextup-core/src/main/kotlin/com/nextup/core/service/game/BattingRecordService.kt` (1곳)
5. `/nextup-core/src/main/kotlin/com/nextup/core/service/game/PitchingRecordService.kt` (1곳)
6. `/nextup-core/src/main/kotlin/com/nextup/core/service/league/LeagueService.kt` (2곳)
7. `/nextup-core/src/main/kotlin/com/nextup/core/service/stats/PlayerStatsService.kt` (1곳)
8. `/nextup-core/src/main/kotlin/com/nextup/core/service/user/UserService.kt` (1곳)

---

## 검증

### 빌드 성공 확인
```bash
./gradlew build -x test
```

**결과:**
- nextup-common: ✅ 빌드 성공
- nextup-core: ✅ 빌드 성공
- nextup-infrastructure: ✅ 빌드 성공
- nextup-api: ✅ 빌드 성공
- nextup-backoffice: ✅ 빌드 성공
- nextup-scorer: ✅ 빌드 성공

---

## 영향 범위

### 1. Hexagonal Architecture 준수
- Core 레이어의 Port 인터페이스만 수정
- Infrastructure 레이어는 JpaRepository 상속으로 자동 호환
- 의존성 방향 유지: Infrastructure → Core (변경 없음)

### 2. 비즈니스 로직 무변경
- Service 레이어의 비즈니스 로직은 변경되지 않음
- Exception 처리 방식 동일 (`.orElseThrow()` 사용)

### 3. 테스트 영향
- 기존 테스트 코드 수정 필요 (Mock 반환값 변경)
- `when(repository.findById(id)).thenReturn(entity)` 
  → `when(repository.findById(id)).thenReturn(Optional.of(entity))`

---

## 다음 단계

1. 테스트 코드 수정 및 실행
2. 통합 테스트 실행으로 E2E 검증
3. PR 생성 및 리뷰 요청

---

## 참고

### Optional 사용의 장점
1. **타입 안정성**: Null 처리 명시적 강제
2. **JPA 표준 준수**: Spring Data JPA와 시그니처 일치
3. **함수형 프로그래밍**: map, flatMap 등 체이닝 가능

### 주의사항
- Kotlin에서 Optional 사용은 일반적으로 권장되지 않지만, JPA 호환성을 위해 Port 인터페이스에만 제한적으로 사용
- Service 레이어에서는 즉시 unwrap하여 Kotlin의 nullable 타입으로 처리

