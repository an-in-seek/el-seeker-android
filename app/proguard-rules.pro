# ElSeeker ProGuard Rules

# Keep BuildConfig
-keep class com.elseeker.android.BuildConfig { *; }

# ──────────────────────────────────────────────────────────────────────────
# Kotlinx Serialization (Retrofit 응답/요청 DTO 직렬화)
# 직렬화는 컴파일러가 생성한 KSerializer 를 리플렉션 없이 사용하지만,
# @Serializable 클래스의 Companion / serializer() 가 제거/난독화되면 깨진다.
-keepattributes *Annotation*
-keepclassmembers @kotlinx.serialization.Serializable class com.elseeker.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.elseeker.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.elseeker.android.**$$serializer { *; }

# Google Play Services Auth (consumer rules 보완)
-keep class com.google.android.gms.auth.api.signin.** { *; }

# release 빌드에서 인증 흐름 등의 Log.v/d/i 호출 제거 (Log.w/e는 유지).
# 토큰 값은 애초에 로깅하지 않지만, 부수적 정보 노출을 release에서 차단한다.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ──────────────────────────────────────────────────────────────────────────
# Retrofit / Kakao SDK (R8 난독화 대응)
#
# 카카오 SDK(v2-user)는 내부적으로 Retrofit을 사용한다. Retrofit은 인터페이스
# 메서드의 "제네릭 반환 타입"(예: Call<UserResponse>)을 런타임 리플렉션으로 읽어
# call adapter를 생성한다. 이 정보는 클래스 파일의 `Signature` 속성에 들어있는데,
# proguard-android-optimize.txt 기본 설정은 Signature 를 유지하지 않는다.
# → 릴리즈 빌드에서 Call<Foo> 가 raw Call 로 보여
#   "Call return type must be parameterized as Call<Foo>" 예외가 발생,
#   앱 포그라운드 진입 시(AppLifecycleObserver.checkAccessToken) 즉시 크래시한다.
#   (디버그 빌드는 minify 미적용이라 재현되지 않음)
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retrofit 인터페이스 메서드의 HTTP 애노테이션 유지
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# 카카오 SDK 모델(직렬화/역직렬화 대상)·내부 API 인터페이스 유지
-keep class com.kakao.sdk.**.model.** { *; }
-keep class com.kakao.sdk.** { *; }
-keep interface com.kakao.sdk.** { *; }
-dontwarn com.kakao.sdk.**

# 네이버 로그인 SDK도 리플렉션/직렬화를 사용하므로 방어적으로 유지
-keep class com.navercorp.nid.** { *; }
-dontwarn com.navercorp.nid.**

# OkHttp (play-app-update / gms-auth 등을 통해 간접 포함)이 참조하는
# 선택적 TLS 프로바이더 클래스들. Android 런타임에는 존재하지 않으므로
# R8 경고를 무시한다 (런타임 동작에는 영향 없음).
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
