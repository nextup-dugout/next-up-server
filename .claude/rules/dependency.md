# 의존성 규칙 (Dependency Rules)

> **절대 규칙 - Reviewer VETO 대상**
> 이 규칙 위반 시 무조건 빌드 REJECT

---

## 📐 Hexagonal Architecture 원칙

```
Outside → Inside (항상 이 방향만 허용)
Core가 Infra를 알면 절대 안 됨
```

### 핵심 개념

| 계층 | 역할 | 의존성 방향 |
|------|------|-------------|
| **Inside (Core)** | 비즈니스 로직 | 외부를 모름 |
| **Outside (Infra/API)** | 어댑터 | Core를 앎 |

---

## 🏗️ Multi-Module Dependency Rules

```
nextup-api → nextup-infrastructure → nextup-core → nextup-common
```

| 모듈 | 허용된 의존성 | 금지 |
|------|---------------|------|
| `nextup-api` | `infra`, `core`, `common` | - |
| `nextup-core` | `common` **ONLY** | ❌ infra, api 절대 금지 |
| `nextup-infrastructure` | `core`, `common` | ❌ api 금지 |
| `nextup-common` | **NONE** (리프 모듈) | ❌ 모든 의존성 금지 |

---

## ⚠️ 절대 금지 사항

### 1. 순환 참조 (Circular Dependency)
```kotlin
// ❌ 절대 금지
// core가 infra를 참조
package com.nextup.core.entity
import com.nextup.infrastructure.repository.PlayerRepository  // VETO!

// ✅ 올바른 방법
// core는 interface만 정의, infra가 구현
package com.nextup.core.port
interface PlayerPort  // core에 정의

package com.nextup.infrastructure.adapter
class PlayerAdapter : PlayerPort  // infra에서 구현
```

### 2. Common에 비즈니스 로직 배치
```kotlin
// ❌ 절대 금지
// common은 공통 유틸리티만
package com.nextup.common.domain
class PlayerScoreCalculator  // VETO! 비즈니스 로직은 core에

// ✅ 올바른 방법
package com.nextup.common.util
object StringUtils  // 순수 유틸리티만
```

---

## 🔍 검증 방법

### Gradle 의존성 체크
```bash
./gradlew dependencies
```

### 검증 기준
1. `nextup-core` 모듈이 `infra` 또는 `api`를 의존하는가? → **즉시 REJECT**
2. `nextup-common` 모듈이 다른 모듈을 의존하는가? → **즉시 REJECT**
3. 순환 참조가 있는가? → **즉시 REJECT**

---

## 📋 체크리스트

Agent/Claude가 코드 작성 전 확인:

- [ ] Core 모듈에서 Infra/API import 하지 않았는가?
- [ ] Common 모듈에 비즈니스 로직 넣지 않았는가?
- [ ] Port/Adapter 패턴을 올바르게 사용했는가?
- [ ] 순환 참조가 없는가?

---

## 🎯 이 규칙의 목적

1. **독립성**: Core는 프레임워크/DB 변경에 영향받지 않음
2. **테스트 용이성**: Core는 단위 테스트만으로 검증 가능
3. **재사용성**: Core는 다른 프로젝트에서도 사용 가능
4. **명확성**: 책임이 명확하여 유지보수 쉬움
