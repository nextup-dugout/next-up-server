# Issue #27 OAuth2 소셜 로그인 - API 계층 구현 완료

## 구현 개요

OAuth2 소셜 로그인 API 계층을 구현하여 사용자가 소셜 계정을 연동하고 관리할 수 있는 기능을 제공합니다.

## 구현된 파일

### 1. DTO 파일 (nextup-api)

#### `/nextup-api/src/main/kotlin/com/nextup/api/dto/oauth/OAuthLinkRequest.kt`
```kotlin
data class OAuthLinkRequest(
    @field:NotNull(message = "OAuth provider is required")
    val provider: OAuthProvider
)
```
- OAuth 계정 연동 요청 DTO
- Provider 검증 포함

#### `/nextup-api/src/main/kotlin/com/nextup/api/dto/oauth/OAuthLinkResponse.kt`
```kotlin
data class OAuthLinkResponse(
    val provider: OAuthProvider,
    val linkedAt: String
)

data class LinkedOAuthAccountsResponse(
    val providers: List<OAuthProvider>
)

data class OAuthLinkStartResponse(
    val authorizationUrl: String,
    val provider: OAuthProvider
)
```
- 3가지 응답 DTO 정의
- Zero Entity Leak 준수 (Entity 직접 노출하지 않음)

### 2. Controller (nextup-api)

#### `/nextup-api/src/main/kotlin/com/nextup/api/controller/oauth/OAuthController.kt`
```kotlin
@RestController
@RequestMapping("/api/me/oauth-accounts")
class OAuthController(
    private val oauthLinkService: OAuthLinkService,
    @Value("\${app.oauth2.base-url:http://localhost:8080}")
    private val baseUrl: String
)
```

**구현된 API 엔드포인트:**
1. `GET /api/me/oauth-accounts` - 연동된 소셜 계정 목록 조회
2. `POST /api/me/oauth-accounts/{provider}/link` - 소셜 계정 연동 시작
3. `DELETE /api/me/oauth-accounts/{provider}` - 소셜 계정 연결 해제

**핵심 구현 사항:**
- `@AuthenticationPrincipal`로 현재 사용자 주입
- ApiResponse로 모든 응답 래핑
- OAuth2 표준 엔드포인트 (`/oauth2/authorization/{registrationId}`) 활용

### 3. SecurityConfig 수정 (nextup-api)

#### `/nextup-api/src/main/kotlin/com/nextup/api/config/SecurityConfig.kt`

**추가된 의존성:**
```kotlin
private val customOAuth2UserService: CustomOAuth2UserService,
private val oAuth2AuthenticationSuccessHandler: OAuth2AuthenticationSuccessHandler,
private val oAuth2AuthenticationFailureHandler: OAuth2AuthenticationFailureHandler
```

**OAuth2 로그인 설정:**
```kotlin
.oauth2Login { oauth2 ->
    oauth2
        .userInfoEndpoint { it.userService(customOAuth2UserService) }
        .successHandler(oAuth2AuthenticationSuccessHandler)
        .failureHandler(oAuth2AuthenticationFailureHandler)
}
```

**추가된 Public Endpoints:**
- `/oauth2/**` - OAuth2 인증 플로우
- `/login/oauth2/**` - OAuth2 콜백

### 4. application.yml 수정 (nextup-api)

#### `/nextup-api/src/main/resources/application.yml`

**OAuth2 클라이언트 설정 추가:**
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID:}
            client-secret: ${KAKAO_CLIENT_SECRET:}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            authorization-grant-type: authorization_code
            client-authentication-method: client_secret_post
            scope:
              - profile_nickname
              - account_email

          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - email
              - profile

          naver:
            client-id: ${NAVER_CLIENT_ID:}
            client-secret: ${NAVER_CLIENT_SECRET:}
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            authorization-grant-type: authorization_code
            scope:
              - name
              - email

        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

          naver:
            authorization-uri: https://nid.naver.com/oauth2.0/authorize
            token-uri: https://nid.naver.com/oauth2.0/token
            user-info-uri: https://openapi.naver.com/v1/nid/me
            user-name-attribute: response

app:
  oauth2:
    base-url: ${APP_BASE_URL:http://localhost:8080}
    redirect-uri: ${OAUTH2_REDIRECT_URI:http://localhost:3000/oauth2/callback}
```

**지원 Provider:**
- Kakao
- Google
- Naver

### 5. 테스트 파일 (nextup-api)

#### `/nextup-api/src/test/kotlin/com/nextup/api/controller/oauth/OAuthControllerTest.kt`
- MockK 기반 Unit Test
- 6개 테스트 케이스 작성
- 성공/실패 시나리오 모두 커버

**테스트 케이스:**
1. 연동된 소셜 계정 목록 조회 성공
2. 연동된 계정이 없는 경우
3. 소셜 계정 연동 시작 성공
4. 소셜 계정 연결 해제 성공
5. 연결되지 않은 계정 해제 시도 시 예외 발생

## 핵심 원칙 준수

### 1. Zero Entity Leak (절대 규칙)
- ✅ 모든 API는 DTO로 응답
- ✅ Entity 직접 노출 없음

### 2. ApiResponse 필수 사용
- ✅ 모든 응답을 `ApiResponse<T>`로 래핑
- ✅ 성공/실패 응답 일관성 유지

### 3. CustomException 사용
- ✅ `OAuthAccountNotFoundException` 처리
- ✅ GlobalExceptionHandler가 자동으로 처리

### 4. 인증/인가
- ✅ `@AuthenticationPrincipal`로 현재 사용자 주입
- ✅ JWT 기반 인증과 OAuth2 통합

## 의존성 방향 검증

```
nextup-api (Controller, DTO, Config, Test)
    ↓
nextup-infrastructure (OAuthLinkService, CustomOAuth2UserService, Handlers)
    ↓
nextup-core (OAuthProvider, User Entity)
    ↓
nextup-common (Exceptions)
```

✅ Outside → Inside 규칙 준수
✅ Core는 Infrastructure/API를 모름

## 빌드 및 테스트 결과

```
./gradlew :nextup-api:build --no-daemon
BUILD SUCCESSFUL in 12s
14 actionable tasks: 11 executed, 3 up-to-date
```

✅ 모든 테스트 통과
✅ 빌드 성공
✅ 컴파일 에러 없음

## API 사용 흐름

### 1. 소셜 계정 연동 시작
```http
POST /api/me/oauth-accounts/kakao/link
Authorization: Bearer {access_token}

Response:
{
  "success": true,
  "data": {
    "authorizationUrl": "http://localhost:8080/oauth2/authorization/kakao",
    "provider": "KAKAO"
  }
}
```

### 2. 사용자가 authorizationUrl로 리다이렉트
- Spring Security OAuth2 Client가 자동 처리
- CustomOAuth2UserService가 사용자 정보 처리
- OAuth2AuthenticationSuccessHandler가 JWT 토큰 발급
- 프론트엔드로 리다이렉트 (토큰 포함)

### 3. 연동된 계정 목록 조회
```http
GET /api/me/oauth-accounts
Authorization: Bearer {access_token}

Response:
{
  "success": true,
  "data": {
    "providers": ["KAKAO", "GOOGLE"]
  }
}
```

### 4. 소셜 계정 연결 해제
```http
DELETE /api/me/oauth-accounts/kakao
Authorization: Bearer {access_token}

Response:
{
  "success": true,
  "data": null
}
```

## 환경 변수 설정 필요

실제 OAuth2 로그인을 사용하려면 다음 환경 변수를 설정해야 합니다:

```bash
# Kakao
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

# Google
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Naver
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

# App URLs
APP_BASE_URL=http://localhost:8080
OAUTH2_REDIRECT_URI=http://localhost:3000/oauth2/callback
```

## 다음 단계 (선택 사항)

1. **Apple 로그인 추가** (향후)
   - OAuthProvider.APPLE 이미 정의됨
   - application.yml에 설정만 추가하면 됨

2. **프론트엔드 통합**
   - OAuth2 콜백 페이지 구현
   - 토큰 저장 및 관리

3. **통합 테스트** (선택)
   - TestContainers 사용
   - 실제 OAuth2 플로우 테스트

## 구현 완료 체크리스트

- ✅ OAuthController 구현
- ✅ DTO 클래스 설계 (Request/Response)
- ✅ SecurityConfig OAuth2 설정 추가
- ✅ application.yml OAuth2 클라이언트 설정
- ✅ Unit Test 작성 (6개)
- ✅ Zero Entity Leak 준수
- ✅ ApiResponse 래핑
- ✅ CustomException 처리
- ✅ 빌드 및 테스트 통과
- ✅ 의존성 방향 규칙 준수

## 구현 날짜

2026-02-02
