# ElSeeker Android Mobile App Documentation

ElSeeker 웹 서비스를 WebView 기반 Android 앱으로 제공하기 위한 기획 및 설계 문서입니다.

## Documents

| 문서 | 설명 |
|------|------|
| [android-app-spec.md](android-app-spec.md) | 기획서 - 요구사항, 기능 범위, MVP 정의, 릴리즈 로드맵 |
| [android-app-architecture.md](android-app-architecture.md) | 설계서 - 아키텍처, 기술 스택, 상세 기술 설계 |

## Context

- ElSeeker는 Kotlin + Spring Boot 3.5.9 기반 Thymeleaf SSR 웹 애플리케이션
- OAuth2 소셜 로그인 (Google, Naver, Kakao) + JWT HttpOnly 쿠키 인증
- 모바일 반응형 웹 (Bottom Tab Bar, Navigation Rail, Safe Area 대응)
- WebView 래퍼 방식으로 최소 네이티브 코드 + 최대 웹 자산 재활용
