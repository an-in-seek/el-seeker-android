# 모바일 소셜 로그인 API 연동 가이드

> **대상**: Android/iOS 모바일 개발자
> **작성일**: 2026-03-17
> **Base URL**: `{EL_SEEKER_API_BASE_URL}`

---

## 1. 개요

모바일 앱에서 네이티브 소셜 SDK로 획득한 토큰을 서버에 전달하면, 서버가 토큰을 검증하고 자체 JWT를 발급하는 방식입니다.

**기존 웹 OAuth2 플로우와의 차이점:**
- 웹: 서버가 OAuth2 Authorization Code 플로우를 직접 수행 → JWT를 HttpOnly 쿠키에 저장
- 모바일: 앱이 네이티브 SDK로 토큰 획득 → 서버 API로 전달 → JWT를 JSON body로 수령

---

## 2. 지원 소셜 Provider

| Provider | 전달 토큰 타입 | provider 값 |
|----------|---------------|-------------|
| Google   | **ID Token** (JWT) | `google` |
| Kakao    | **Access Token** | `kakao` |
| Naver    | **Access Token** | `naver` |

---

## 3. 소셜 로그인 API

### `POST /api/v1/auth/social-login`

인증 불필요 (permitAll)

#### Headers

```
Content-Type: application/json
```

#### Request Body

```json
{
  "provider": "google",
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `provider` | String | O | 소셜 Provider (`google`, `kakao`, `naver`) |
| `token` | String | O | 소셜 SDK에서 획득한 토큰 |

#### Response (200 OK)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | String | JWT Access Token (API 호출 시 사용) |
| `refreshToken` | String | JWT Refresh Token (Access Token 갱신 시 사용) |

#### Error Responses

에러 응답 JSON 형식:
```json
{
  "status": 401,
  "message": "소셜 로그인 토큰이 유효하지 않습니다."
}
```

| HTTP Status | ErrorType | message | 설명 |
|-------------|-----------|---------|------|
| 401 | `SOCIAL_LOGIN_INVALID_TOKEN` | 소셜 로그인 토큰이 유효하지 않습니다. | 토큰 만료, 서명 오류, audience 불일치 등 |
| 400 | `OAUTH_EMAIL_MISSING` | 소셜 로그인 이메일 정보를 찾을 수 없습니다. | 소셜 계정에 이메일 미제공 (Kakao 이메일 미동의 등) |
| 400 | `INVALID_PARAMETER` | 잘못된 요청 파라미터입니다. | `provider` 값이 google/kakao/naver가 아닌 경우 또는 빈 값 |

---

## 4. 인증 후 API 호출 방법

소셜 로그인으로 발급받은 JWT는 **`Authorization` 헤더**에 Bearer 토큰으로 전달합니다.

```
Authorization: Bearer {accessToken}
```

서버는 토큰을 다음 우선순위로 읽습니다:
1. `Authorization: Bearer` 헤더 (모바일)
2. `ACCESS_TOKEN` 쿠키 (웹)

> **주의**: 토큰을 앱 내부 보안 저장소에 저장하세요.
> - Android: `EncryptedSharedPreferences`
> - iOS: `Keychain`

### Access Token JWT Payload 구조 (참고)

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",  // memberUid
  "email": "user@example.com",
  "roles": ["USER"],
  "iat": 1742198400,
  "exp": 1742202000
}
```

---

## 5. Access Token 갱신

### `POST /api/v1/auth/refresh` (현재 모바일 미지원)

> **주의**: 현재 Refresh 엔드포인트는 **쿠키 전용**입니다.
> - Access Token: `Authorization: Bearer` 헤더 지원 (모바일 사용 가능)
> - Refresh Token: `REFRESH_TOKEN` 쿠키에서만 읽음 (모바일 사용 **불가**)
>
> **임시 대응 방안**: Access Token 만료 시 소셜 로그인을 다시 수행하여 새 토큰 세트를 발급받습니다.
>
> **향후 계획**: 모바일용 Refresh 엔드포인트 추가 예정 (Request Body로 Refresh Token 전달 방식)

---

## 6. 내 정보 조회

### `GET /api/v1/auth/me`

인증 필요 (`Authorization: Bearer {accessToken}`)

#### Response (200 OK)

```json
{
  "memberUid": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "USER",
  "nickname": "홍길동",
  "profileImageUrl": "https://...",
  "provider": "google",
  "createdAt": "2026-03-17T12:00:00Z"
}
```

---

## 7. Provider별 네이티브 SDK 설정 가이드

### 7-1. Google (Android)

```kotlin
// build.gradle
implementation("com.google.android.gms:play-services-auth:21.3.0")
// 또는 Credential Manager 사용 권장

// Google Sign-In 설정
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestIdToken(SERVER_CLIENT_ID)  // ⚠️ 서버(웹)의 Google Client ID 사용
    .requestEmail()
    .build()
```

> **중요**: `requestIdToken()`에 전달하는 Client ID는 **Android 앱의 Client ID가 아니라 서버(웹)의 Client ID**입니다.
> 서버에서 google-auth-library를 사용하여 ID Token의 서명을 로컬 검증하며, audience(`aud`)가 서버 Client ID와 일치하는지 확인합니다.

```kotlin
// 로그인 성공 후 ID Token 추출
val account = GoogleSignIn.getSignedInAccountFromIntent(data).result
val idToken = account.idToken  // 이 값을 서버에 전달
```

### 7-2. Kakao (Android)

```kotlin
// build.gradle
implementation("com.kakao.sdk:v2-user:2.20.6")

// 로그인 후 Access Token 추출
UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
    if (token != null) {
        val accessToken = token.accessToken  // 이 값을 서버에 전달
    }
}
```

### 7-3. Naver (Android)

```kotlin
// build.gradle
implementation("com.navercorp.nid:oauth-jdk8:5.10.0")

// 로그인 후 Access Token 추출
NaverIdLoginSDK.authenticate(context, object : OAuthLoginCallback {
    override fun onSuccess() {
        val accessToken = NaverIdLoginSDK.getAccessToken()  // 이 값을 서버에 전달
    }
    // ...
})
```

---

## 8. 서버 회원 처리 로직 (참고)

서버는 소셜 로그인 시 다음 순서로 회원을 처리합니다:

1. **기존 OAuth 계정 조회**: provider + providerUserId로 매칭 → OAuth 프로필 동기화 후 JWT 발급
2. **동일 이메일 회원 존재**: 기존 회원에 OAuth 계정 자동 연결 + 닉네임/프로필 이미지가 비어 있으면 소셜 정보로 초기화 → JWT 발급
3. **신규 사용자**: 회원 + OAuth 계정 생성 + 소셜 닉네임/프로필 이미지로 초기화 → JWT 발급

> 소셜 계정에 닉네임이 없는 경우(예: 일부 Naver 계정) 닉네임이 빈 문자열일 수 있으므로, 앱에서 닉네임이 비어 있으면 설정 화면으로 유도하는 것을 권장합니다.

---

## 9. 전체 시퀀스 다이어그램

```
┌──────┐         ┌──────────┐         ┌──────────┐         ┌──────────────┐
│ User │         │  App     │         │  Server  │         │ Social API   │
└──┬───┘         └────┬─────┘         └────┬─────┘         └──────┬───────┘
   │  소셜 로그인 탭   │                    │                      │
   │ ────────────────>│                    │                      │
   │                  │  네이티브 SDK 호출   │                      │
   │                  │ ──────────────────────────────────────────>│
   │                  │           소셜 토큰 반환                    │
   │                  │ <──────────────────────────────────────────│
   │                  │                    │                      │
   │                  │  POST /api/v1/auth/social-login            │
   │                  │  {provider, token} │                      │
   │                  │ ──────────────────>│                      │
   │                  │                    │  토큰 검증 API 호출    │
   │                  │                    │ ────────────────────>│
   │                  │                    │       검증 결과        │
   │                  │                    │ <────────────────────│
   │                  │                    │                      │
   │                  │  {accessToken,     │                      │
   │                  │   refreshToken}    │                      │
   │                  │ <──────────────────│                      │
   │                  │                    │                      │
   │                  │  이후 API 호출      │                      │
   │                  │  Authorization:    │                      │
   │                  │  Bearer {token}    │                      │
   │                  │ ──────────────────>│                      │
```

---

## 10. 체크리스트

- [ ] Google Cloud Console에서 Android 앱용 OAuth Client ID 생성
- [ ] Kakao Developers에서 Android 플랫폼 등록 (키 해시 등록)
- [ ] Naver Developers에서 Android 앱 등록
- [ ] 토큰 보안 저장소 구현 (EncryptedSharedPreferences / Keychain)
- [ ] Access Token 만료 시 재로그인 처리 구현
- [ ] 신규 가입 후 닉네임 설정 화면 구현
- [ ] 네트워크 에러 / 토큰 검증 실패 에러 핸들링
